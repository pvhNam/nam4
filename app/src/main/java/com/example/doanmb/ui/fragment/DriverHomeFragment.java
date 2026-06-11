package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.adapter.TripAdapter;
import com.example.doanmb.model.Trip;
import com.example.doanmb.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Trang chủ tài xế: số chuyến & doanh thu hôm nay + danh sách đơn đang chờ nhận
 * (lọc theo loại xe của tài xế). Bấm "Nhận chuyến" để nhận.
 */
public class DriverHomeFragment extends Fragment implements TripAdapter.OnTripActionListener {

    private TextView tvTripsToday, tvRevenueToday, tvEmpty, tvOfflineHint;
    private RecyclerView rvWaiting;
    private TripAdapter adapter;
    private final List<Trip> waitingTrips = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid, driverName, driverCarType;
    private boolean online;

    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_home, container, false);
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";

        tvTripsToday = view.findViewById(R.id.tv_trips_today);
        tvRevenueToday = view.findViewById(R.id.tv_revenue_today);
        tvEmpty = view.findViewById(R.id.tv_empty_waiting);
        tvOfflineHint = view.findViewById(R.id.tv_offline_hint);
        rvWaiting = view.findViewById(R.id.rv_waiting_trips);

        adapter = new TripAdapter(waitingTrips, this);
        rvWaiting.setLayoutManager(new LinearLayoutManager(getContext()));
        rvWaiting.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDriverProfileThenData();
    }

    private void loadDriverProfileThenData() {
        if (uid.isEmpty()) return;
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            driverName = doc.getString("name");
            driverCarType = doc.getString("driverCarType");
            Boolean on = doc.getBoolean("driverOnline");
            online = on != null && on;

            tvOfflineHint.setVisibility(online ? View.GONE : View.VISIBLE);
            loadWaitingTrips();
            loadTodayStats();
        });
    }

    /** Đơn đang chờ nhận, lọc theo loại xe của tài xế (nếu offline thì để trống). */
    private void loadWaitingTrips() {
        waitingTrips.clear();
        if (!online) {
            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Bạn đang offline");
            return;
        }
        db.collection("trips")
                .whereEqualTo("status", Trip.STATUS_WAITING)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    waitingTrips.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Trip t = d.toObject(Trip.class);
                        t.setId(d.getId());
                        // Chỉ hiện đơn cùng loại xe (nếu tài xế chưa khai loại xe thì hiện tất cả)
                        if (driverCarType == null || driverCarType.isEmpty()
                                || driverCarType.equals(t.getCarType())) {
                            waitingTrips.add(t);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setText("Chưa có đơn nào đang chờ");
                    tvEmpty.setVisibility(waitingTrips.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    /** Đếm chuyến & cộng doanh thu các chuyến đã hoàn thành trong hôm nay. */
    private void loadTodayStats() {
        db.collection("trips")
                .whereEqualTo("driverId", uid)
                .whereEqualTo("status", Trip.STATUS_COMPLETED)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    Date startOfDay = startOfToday();
                    int count = 0;
                    double revenue = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Timestamp done = d.getTimestamp("completedAt");
                        if (done != null && done.toDate().after(startOfDay)) {
                            count++;
                            Double price = d.getDouble("price");
                            if (price != null) revenue += price;
                        }
                    }
                    tvTripsToday.setText(String.valueOf(count));
                    tvRevenueToday.setText(MONEY.format(revenue) + " đ");
                });
    }

    private Date startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    /** Tài xế bấm "Nhận chuyến". Dùng transaction để tránh 2 tài xế nhận cùng 1 đơn. */
    @Override
    public void onAction(Trip trip) {
        if (trip.getId() == null) return;
        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(db.collection("trips").document(trip.getId()));
            String status = snap.getString("status");
            if (!Trip.STATUS_WAITING.equals(status)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Đơn đã được nhận",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            transaction.update(db.collection("trips").document(trip.getId()),
                    "status", Trip.STATUS_RUNNING,
                    "driverId", uid,
                    "driverName", driverName != null ? driverName : "",
                    "acceptedAt", Timestamp.now());
            return null;
        }).addOnSuccessListener(v -> {
            if (!isAdded()) return;
            Toast.makeText(getContext(), "✅ Đã nhận chuyến! Xem ở mục Chuyến của tôi.", Toast.LENGTH_SHORT).show();
            loadWaitingTrips();
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Toast.makeText(getContext(), "Không nhận được: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadWaitingTrips();
        });
    }
}
