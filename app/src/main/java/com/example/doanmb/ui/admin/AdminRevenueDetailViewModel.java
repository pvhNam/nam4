package com.example.doanmb.ui.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.doanmb.data.FirebaseContract.OrderStatus;
import com.example.doanmb.data.repository.CarRepository;
import com.example.doanmb.data.repository.OrderRepository;
import com.example.doanmb.ui.base.BaseViewModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel màn chi tiết doanh thu: nạp đơn đã xác nhận + bài đăng, tính tổng
 * hoa hồng / phí đăng. Việc dựng item hiển thị & định dạng vẫn ở Activity.
 */
public class AdminRevenueDetailViewModel extends BaseViewModel {

    public static final double SALE_COMMISSION   = 0.03;
    public static final double RENTAL_COMMISSION = 0.15;
    public static final long   POSTING_FEE       = 200_000L;

    /** Gói dữ liệu phát ra UI khi cả 2 nguồn đã sẵn sàng. */
    public static class Data {
        public final List<DocumentSnapshot> orders;
        public final List<DocumentSnapshot> cars;
        public final long grandTotal, commissionTotal, postingTotal;
        Data(List<DocumentSnapshot> o, List<DocumentSnapshot> c, long g, long cm, long p) {
            orders = o; cars = c; grandTotal = g; commissionTotal = cm; postingTotal = p;
        }
    }

    private final OrderRepository orderRepo = OrderRepository.getInstance();
    private final CarRepository carRepo = CarRepository.getInstance();
    private final MutableLiveData<Data> data = new MutableLiveData<>();

    public LiveData<Data> getData() { return data; }

    public void load() {
        setLoading(true);
        final List<DocumentSnapshot>[] holder = new List[2];
        final int[] done = {0};
        Runnable check = () -> {
            if (done[0] < 2) return;
            setLoading(false);
            List<DocumentSnapshot> orders = holder[0] != null ? holder[0] : new ArrayList<>();
            List<DocumentSnapshot> cars   = holder[1] != null ? holder[1] : new ArrayList<>();
            long commission = 0;
            for (DocumentSnapshot d : orders) commission += commissionOf(d);
            long posting = (long) cars.size() * POSTING_FEE;
            data.setValue(new Data(orders, cars, commission + posting, commission, posting));
        };
        orderRepo.getOrdersByStatus(OrderStatus.CONFIRMED, r -> {
            holder[0] = r.isSuccess() ? r.getData() : new ArrayList<>();
            done[0]++; check.run();
        });
        carRepo.getAllCarDocs(r -> {
            holder[1] = r.isSuccess() ? r.getData() : new ArrayList<>();
            done[0]++; check.run();
        });
    }

    /** Hoa hồng của 1 đơn (dùng cho cả tính tổng và hiển thị từng dòng). */
    public long commissionOf(@NonNull DocumentSnapshot doc) {
        long price = parsePrice(doc.getString("carPrice"));
        if ("Thuê xe".equals(doc.getString("type"))) {
            long days = 1;
            String daysStr = doc.getString("days");
            if (daysStr != null) {
                try {
                    long d = Long.parseLong(daysStr.replaceAll("[^0-9]", ""));
                    if (d > 0) days = d;
                } catch (NumberFormatException ignored) {}
            }
            return (long) (price * days * RENTAL_COMMISSION);
        }
        return (long) (price * SALE_COMMISSION);
    }

    private long parsePrice(String s) {
        if (s == null || s.isEmpty()) return 0;
        String d = s.replaceAll("[^0-9]", "");
        if (d.isEmpty()) return 0;
        try { return Long.parseLong(d); } catch (NumberFormatException e) { return 0; }
    }
}
