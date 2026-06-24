package com.example.doanmb.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.DriverStatus;
import com.example.doanmb.data.FirebaseContract.OrderStatus;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.data.repository.OrderRepository;
import com.example.doanmb.data.repository.UserRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.List;

/**
 * ViewModel cho màn Tổng quan Admin: tính các con số doanh thu (hoa hồng bán 3%,
 * thuê 15%, phí đăng bài) và dữ liệu biểu đồ 6 tháng. Toàn bộ tính toán nằm ở đây,
 * Fragment chỉ hiển thị.
 */
public class AdminOverviewViewModel extends BaseViewModel {

    public static final double SALE_COMMISSION   = 0.03;
    public static final double RENTAL_COMMISSION = 0.15;
    public static final long   POSTING_FEE       = 200_000;

    /** Gói số liệu tổng quan để truyền 1 lần ra UI. */
    public static class Summary {
        public long total, month, sale, rental, postingFee;
        public int confirmedCount, carCount;
    }

    /** Dữ liệu biểu đồ 6 tháng (doanh thu thô + nhãn tháng). */
    public static class ChartData {
        public final long[] monthRevenue;
        public final String[] labels;
        public ChartData(long[] m, String[] l) { monthRevenue = m; labels = l; }
    }

    private final OrderRepository orderRepo = OrderRepository.getInstance();
    private final CarRepository carRepo = CarRepository.getInstance();
    private final UserRepository userRepo = UserRepository.getInstance();

    private final MutableLiveData<Summary> summary = new MutableLiveData<>();
    private final MutableLiveData<ChartData> chart = new MutableLiveData<>();
    private final MutableLiveData<Integer> driverPending = new MutableLiveData<>();

    public LiveData<Summary> getSummary()       { return summary; }
    public LiveData<ChartData> getChart()        { return chart; }
    public LiveData<Integer> getDriverPending()  { return driverPending; }

    public void load() {
        loadRevenueAndChart();
        loadDriverPending();
    }

    private void loadDriverPending() {
        userRepo.getUsersByDriverStatus(DriverStatus.PENDING, r -> {
            if (r.isSuccess() && r.getData() != null) driverPending.setValue(r.getData().size());
        });
    }

    private void loadRevenueAndChart() {
        setLoading(true);
        orderRepo.getOrdersByStatus(OrderStatus.CONFIRMED, r -> {
            if (!r.isSuccess() || r.getData() == null) {
                setLoading(false);
                postMessage("Lỗi tải doanh thu: " + r.getError());
                return;
            }
            List<DocumentSnapshot> orders = r.getData();
            Summary s = computeSummary(orders);
            ChartData cd = computeChart(orders);

            // Chain: lấy số bài đăng để tính phí đăng & tổng
            carRepo.getAllCarDocs(cr -> {
                setLoading(false);
                int carCount = (cr.isSuccess() && cr.getData() != null) ? cr.getData().size() : 0;
                s.carCount = carCount;
                s.postingFee = (long) carCount * POSTING_FEE;
                s.total = commissionTotal(orders) + s.postingFee;
                summary.setValue(s);
                chart.setValue(cd);
            });
        });
    }

    private long commissionTotal(List<DocumentSnapshot> orders) {
        long sum = 0;
        for (DocumentSnapshot d : orders) sum += commissionOf(d);
        return sum;
    }

    private Summary computeSummary(List<DocumentSnapshot> orders) {
        Summary s = new Summary();
        s.confirmedCount = orders.size();
        Calendar now = Calendar.getInstance();
        int curMonth = now.get(Calendar.MONTH), curYear = now.get(Calendar.YEAR);
        for (DocumentSnapshot d : orders) {
            long commission = commissionOf(d);
            if (isRental(d)) s.rental += commission; else s.sale += commission;
            Timestamp ts = d.getTimestamp("createdAt");
            if (ts != null) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(ts.toDate().getTime());
                if (c.get(Calendar.MONTH) == curMonth && c.get(Calendar.YEAR) == curYear)
                    s.month += commission;
            }
        }
        return s;
    }

    private ChartData computeChart(List<DocumentSnapshot> orders) {
        Calendar now = Calendar.getInstance();
        long[] monthRevenue = new long[6];
        String[] labels = new String[6];
        String[] names = {"T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"};
        for (int i = 5; i >= 0; i--) {
            Calendar c = (Calendar) now.clone();
            c.add(Calendar.MONTH, -i);
            labels[5 - i] = names[c.get(Calendar.MONTH)];
        }
        for (DocumentSnapshot d : orders) {
            Timestamp ts = d.getTimestamp("createdAt");
            if (ts == null) continue;
            Calendar oc = Calendar.getInstance();
            oc.setTimeInMillis(ts.toDate().getTime());
            for (int i = 5; i >= 0; i--) {
                Calendar c = (Calendar) now.clone();
                c.add(Calendar.MONTH, -i);
                if (oc.get(Calendar.MONTH) == c.get(Calendar.MONTH)
                        && oc.get(Calendar.YEAR) == c.get(Calendar.YEAR)) {
                    monthRevenue[5 - i] += commissionOf(d);
                    break;
                }
            }
        }
        return new ChartData(monthRevenue, labels);
    }

    private boolean isRental(DocumentSnapshot d) { return "Thuê xe".equals(d.getString("type")); }

    private long commissionOf(DocumentSnapshot d) {
        long price = parsePrice(d.getString("carPrice"));
        if (isRental(d)) {
            long days = 1;
            String daysStr = d.getString("days");
            if (daysStr != null && !daysStr.isEmpty()) {
                try {
                    long dd = Long.parseLong(daysStr.replaceAll("[^0-9]", ""));
                    if (dd > 0) days = dd;
                } catch (NumberFormatException ignored) {}
            }
            return (long) (price * days * RENTAL_COMMISSION);
        }
        return (long) (price * SALE_COMMISSION);
    }

    private long parsePrice(String s) {
        if (s == null || s.isEmpty()) return 0;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try { return Long.parseLong(digits); } catch (NumberFormatException e) { return 0; }
    }
}
