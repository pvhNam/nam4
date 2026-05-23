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
import com.example.doanmb.adapter.OrderAdminAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StaffOrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private TextView tvOrderCount, tvEmpty;
    private OrderAdminAdapter adapter;
    private List<Map<String, Object>> orderList = new ArrayList<>();
    private List<String> orderIds = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_orders, container, false);
        db = FirebaseFirestore.getInstance();

        rvOrders = view.findViewById(R.id.rv_orders_staff);
        tvOrderCount = view.findViewById(R.id.tv_order_count);
        tvEmpty = view.findViewById(R.id.tv_empty_orders);

        adapter = new OrderAdminAdapter(orderList, orderIds);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);

        loadOrders();
        return view;
    }

    private void loadOrders() {
        db.collection("orders").get().addOnSuccessListener(snapshots -> {
            if (!isAdded()) return;
            orderList.clear();
            orderIds.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                orderList.add(doc.getData());
                orderIds.add(doc.getId());
            }
            adapter.updateList(orderList, orderIds);
            tvOrderCount.setText(orderList.size() + " đơn");
            tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
            rvOrders.setVisibility(orderList.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}
