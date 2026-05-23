package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.CarAdminAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StaffApproveFragment extends Fragment {

    private RecyclerView rvApprove;
    private TextView tvApproveCount, tvEmpty;
    private CarAdminAdapter adapter;
    private List<Map<String, Object>> carList = new ArrayList<>();
    private List<String> carIds = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_approve, container, false);
        db = FirebaseFirestore.getInstance();

        rvApprove = view.findViewById(R.id.rv_approve);
        tvApproveCount = view.findViewById(R.id.tv_approve_count);
        tvEmpty = view.findViewById(R.id.tv_empty_approve);

        adapter = new CarAdminAdapter(carList, carIds);
        rvApprove.setLayoutManager(new LinearLayoutManager(getContext()));
        rvApprove.setAdapter(adapter);

        loadCars();
        return view;
    }

    private void loadCars() {
        db.collection("cars").get().addOnSuccessListener(snapshots -> {
            if (!isAdded()) return;
            carList.clear();
            carIds.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                carList.add(doc.getData());
                carIds.add(doc.getId());
            }
            adapter.updateList(carList, carIds);
            tvApproveCount.setText(carList.size() + " xe");
            tvEmpty.setVisibility(carList.isEmpty() ? View.VISIBLE : View.GONE);
            rvApprove.setVisibility(carList.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCars();
    }
}
