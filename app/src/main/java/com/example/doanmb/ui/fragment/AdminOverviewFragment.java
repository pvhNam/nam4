package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doanmb.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminOverviewFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalCars, tvPendingOrders, tvActiveCars;
    private Button btnViewUsers, btnViewCars;
    private FirebaseFirestore db;

    public interface OnQuickNavListener {
        void navigateTo(int itemId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_overview, container, false);
        db = FirebaseFirestore.getInstance();

        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvTotalCars = view.findViewById(R.id.tv_total_cars);
        tvPendingOrders = view.findViewById(R.id.tv_pending_orders);
        tvActiveCars = view.findViewById(R.id.tv_active_cars);
        btnViewUsers = view.findViewById(R.id.btn_view_users);
        btnViewCars = view.findViewById(R.id.btn_view_cars);
        Button btnViewOrders = view.findViewById(R.id.btn_view_orders);

        loadStats();

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
}
