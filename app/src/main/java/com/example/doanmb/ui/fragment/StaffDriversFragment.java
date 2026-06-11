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
import com.example.doanmb.adapter.DriverApprovalAdapter;
import com.example.doanmb.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/** Admin/Staff duyệt hồ sơ đăng ký tài xế (driverStatus == "pending"). */
public class StaffDriversFragment extends Fragment implements DriverApprovalAdapter.OnDecisionListener {

    private RecyclerView rv;
    private TextView tvEmpty;
    private DriverApprovalAdapter adapter;
    private final List<User> pending = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_drivers, container, false);
        db = FirebaseFirestore.getInstance();

        rv = view.findViewById(R.id.rv_pending_drivers);
        tvEmpty = view.findViewById(R.id.tv_empty_drivers);

        adapter = new DriverApprovalAdapter(pending, this);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        loadPending();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPending();
    }

    private void loadPending() {
        db.collection("users").whereEqualTo("driverStatus", "pending").get()
                .addOnSuccessListener(snap -> {
                    if (!isAdded()) return;
                    pending.clear();
                    for (QueryDocumentSnapshot d : snap) {
                        User u = d.toObject(User.class);
                        u.setUid(d.getId());
                        pending.add(u);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(pending.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onApprove(User u) {
        db.collection("users").document(u.getUid())
                .update("driverStatus", "approved", "role", "DRIVER")
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "✅ Đã duyệt: " + u.getName(), Toast.LENGTH_SHORT).show();
                    loadPending();
                })
                .addOnFailureListener(e -> toastErr(e.getMessage()));
    }

    @Override
    public void onReject(User u) {
        db.collection("users").document(u.getUid())
                .update("driverStatus", "rejected")
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Đã từ chối: " + u.getName(), Toast.LENGTH_SHORT).show();
                    loadPending();
                })
                .addOnFailureListener(e -> toastErr(e.getMessage()));
    }

    private void toastErr(String msg) {
        if (isAdded()) Toast.makeText(getContext(), "Lỗi: " + msg, Toast.LENGTH_SHORT).show();
    }
}
