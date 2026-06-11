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

import com.example.doanmb.R;
import com.example.doanmb.adapter.TripAdapter;
import com.example.doanmb.model.Trip;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * "Chuyến của tôi": chuyến đang chạy (có nút Hoàn thành) và chuyến đã hoàn thành.
 */
public class DriverTripsFragment extends Fragment {

    private RecyclerView rvRunning, rvCompleted;
    private TextView tvEmptyRunning, tvEmptyCompleted;
    private TripAdapter runningAdapter, completedAdapter;
    private final List<Trip> runningTrips = new ArrayList<>();
    private final List<Trip> completedTrips = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_trips, container, false);
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";

        rvRunning = view.findViewById(R.id.rv_running);
        rvCompleted = view.findViewById(R.id.rv_completed);
        tvEmptyRunning = view.findViewById(R.id.tv_empty_running);
        tvEmptyCompleted = view.findViewById(R.id.tv_empty_completed);

        // Nút trên chuyến đang chạy = Hoàn thành chuyến
        runningAdapter = new TripAdapter(runningTrips, this::completeTrip);
        completedAdapter = new TripAdapter(completedTrips, t -> { /* không hành động */ });

        rvRunning.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRunning.setAdapter(runningAdapter);
        rvCompleted.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCompleted.setAdapter(completedAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyTrips();
    }

    private void loadMyTrips() {
        if (uid.isEmpty()) return;
        db.collection("trips").whereEqualTo("driverId", uid).get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    runningTrips.clear();
                    completedTrips.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        Trip t = d.toObject(Trip.class);
                        t.setId(d.getId());
                        if (Trip.STATUS_RUNNING.equals(t.getStatus())) {
                            runningTrips.add(t);
                        } else if (Trip.STATUS_COMPLETED.equals(t.getStatus())) {
                            completedTrips.add(t);
                        }
                    }
                    runningAdapter.notifyDataSetChanged();
                    completedAdapter.notifyDataSetChanged();
                    tvEmptyRunning.setVisibility(runningTrips.isEmpty() ? View.VISIBLE : View.GONE);
                    tvEmptyCompleted.setVisibility(completedTrips.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void completeTrip(Trip trip) {
        if (trip.getId() == null) return;
        db.collection("trips").document(trip.getId())
                .update("status", Trip.STATUS_COMPLETED, "completedAt", Timestamp.now())
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "✅ Đã hoàn thành chuyến!", Toast.LENGTH_SHORT).show();
                    loadMyTrips();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
