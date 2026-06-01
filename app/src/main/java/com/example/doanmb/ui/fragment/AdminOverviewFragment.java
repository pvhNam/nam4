package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.doanmb.ui.activity.AdminRevenueDetailActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doanmb.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;

public class AdminOverviewFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalCars, tvPendingOrders, tvActiveCars;
    private TextView tvTotalRevenue, tvMonthRevenue, tvConfirmedCount, tvSaleRevenue, tvRentalRevenue;
    private TextView tvPostingFeeRevenue, tvPostingCarCount;
    private View btnViewUsers, btnViewCars;
    private FirebaseFirestore db;

    public interface OnQuickNavListener {
        void navigateTo(int itemId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_overview, container, false);
        db = FirebaseFirestore.getInstance();

        tvTotalUsers    = view.findViewById(R.id.tv_total_users);
        tvTotalCars     = view.findViewById(R.id.tv_total_cars);
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders);
        tvActiveCars    = view.findViewById(R.id.tv_active_cars);

        tvTotalRevenue      = view.findViewById(R.id.tv_total_revenue);
        tvMonthRevenue      = view.findViewById(R.id.tv_month_revenue);
        tvConfirmedCount    = view.findViewById(R.id.tv_confirmed_count);
        tvSaleRevenue       = view.findViewById(R.id.tv_sale_revenue);
        tvRentalRevenue     = view.findViewById(R.id.tv_rental_revenue);
        tvPostingFeeRevenue = view.findViewById(R.id.tv_posting_fee_revenue);
        tvPostingCarCount   = view.findViewById(R.id.tv_posting_car_count);

        btnViewUsers = view.findViewById(R.id.btn_view_users);
        btnViewCars  = view.findViewById(R.id.btn_view_cars);
        View btnViewOrders = view.findViewById(R.id.btn_view_orders);

        loadStats();
        loadRevenue();

        view.findViewById(R.id.tv_view_revenue_detail).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AdminRevenueDetailActivity.class)));

        btnViewUsers.setOnClickListener(v -> navigate(R.id.nav_admin_users));
        btnViewCars.setOnClickListener(v -> navigate(R.id.nav_admin_cars));
        btnViewOrders.setOnClickListener(v -> navigate(R.id.nav_admin_orders));

        return view;
    }

    private void navigate(int itemId) {
        if (getActivity() instanceof OnQuickNavListener) {
            ((OnQuickNavListener) getActivity()).navigateTo(itemId);
        }
    }

    private void loadStats() {
        db.collection("users").get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            tvTotalUsers.setText(String.valueOf(snap.size()));
        });

        db.collection("cars").get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            tvTotalCars.setText(String.valueOf(snap.size()));
            long active = snap.getDocuments().stream()
                    .filter(d -> "active".equals(d.getString("status"))).count();
            tvActiveCars.setText(String.valueOf(active));
        });

        db.collection("orders").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    tvPendingOrders.setText(String.valueOf(snap.size()));
                });
    }

    private static final double SALE_COMMISSION   = 0.03;   // 3% giá xe
    private static final double RENTAL_COMMISSION = 0.15;   // 15% phí thuê
    private static final long   POSTING_FEE       = 200_000; // 200K/bài đăng

    private void loadRevenue() {
        db.collection("orders").whereEqualTo("status", "confirmed").get()
                .addOnSuccessListener(orderSnap -> {
                    if (!isAdded()) return;

                    long commissionRevenue = 0;
                    long monthRevenue      = 0;
                    long saleRevenue       = 0;
                    long rentalRevenue     = 0;

                    Calendar now = Calendar.getInstance();
                    int currentMonth = now.get(Calendar.MONTH);
                    int currentYear  = now.get(Calendar.YEAR);

                    for (QueryDocumentSnapshot doc : orderSnap) {
                        String type        = doc.getString("type");
                        long   pricePerUnit = parsePrice(doc.getString("carPrice"));
                        long   commission;

                        if ("Thuê xe".equals(type)) {
                            long days = 1;
                            String daysStr = doc.getString("days");
                            if (daysStr != null && !daysStr.isEmpty()) {
                                try {
                                    long d = Long.parseLong(daysStr.replaceAll("[^0-9]", ""));
                                    if (d > 0) days = d;
                                } catch (NumberFormatException ignored) {}
                            }
                            commission = (long)(pricePerUnit * days * RENTAL_COMMISSION);
                            rentalRevenue += commission;
                        } else {
                            commission = (long)(pricePerUnit * SALE_COMMISSION);
                            saleRevenue += commission;
                        }

                        commissionRevenue += commission;

                        Timestamp ts = doc.getTimestamp("createdAt");
                        if (ts != null) {
                            Calendar orderCal = Calendar.getInstance();
                            orderCal.setTimeInMillis(ts.toDate().getTime());
                            if (orderCal.get(Calendar.MONTH) == currentMonth
                                    && orderCal.get(Calendar.YEAR) == currentYear) {
                                monthRevenue += commission;
                            }
                        }
                    }

                    tvConfirmedCount.setText(String.valueOf(orderSnap.size()));
                    tvSaleRevenue.setText(formatRevenue(saleRevenue));
                    tvRentalRevenue.setText(formatRevenue(rentalRevenue));

                    final long commissionFinal = commissionRevenue;
                    final long monthFinal      = monthRevenue;

                    // Chain: lấy số bài đăng để tính phí
                    db.collection("cars").get().addOnSuccessListener(carSnap -> {
                        if (!isAdded()) return;
                        int carCount = carSnap.size();
                        long postingFee   = carCount * POSTING_FEE;
                        long grandTotal   = commissionFinal + postingFee;

                        tvTotalRevenue.setText(formatRevenue(grandTotal));
                        tvMonthRevenue.setText(formatRevenue(monthFinal));
                        tvPostingFeeRevenue.setText(formatRevenue(postingFee));
                        tvPostingCarCount.setText(carCount + " bài đăng × 200.000 VNĐ");
                    });
                });
    }

    private long parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return 0;
        String digits = priceStr.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatRevenue(long amount) {
        if (amount == 0) return "0 VNĐ";
        if (amount >= 1_000_000_000L) {
            return String.format("%.1f tỷ", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000L) {
            return String.format("%.0f triệu", amount / 1_000_000.0);
        } else {
            return (amount / 1_000) + "K VNĐ";
        }
    }
}
