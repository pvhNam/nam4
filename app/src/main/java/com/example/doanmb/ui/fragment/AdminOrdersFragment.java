package com.example.doanmb.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.OrderAdminAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminOrdersFragment extends Fragment {

    private static final int TAB_PENDING   = 0;
    private static final int TAB_CONFIRMED = 1;
    private static final int TAB_ALL       = 2;

    private RecyclerView rvOrders;
    private TextView tvCount, tvEmpty;
    private Button btnTabPending, btnTabConfirmed, btnTabAll;
    private OrderAdminAdapter adapter;
    private final List<Map<String, Object>> orderList = new ArrayList<>();
    private final List<String> orderIds = new ArrayList<>();
    private FirebaseFirestore db;
    private int currentTab = TAB_PENDING;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_orders, container, false);
        db = FirebaseFirestore.getInstance();

        rvOrders       = view.findViewById(R.id.rv_admin_orders);
        tvCount        = view.findViewById(R.id.tv_admin_order_count);
        tvEmpty        = view.findViewById(R.id.tv_admin_empty_orders);
        btnTabPending  = view.findViewById(R.id.btn_order_tab_pending);
        btnTabConfirmed= view.findViewById(R.id.btn_order_tab_confirmed);
        btnTabAll      = view.findViewById(R.id.btn_order_tab_all);

        adapter = new OrderAdminAdapter(orderList, orderIds, new OrderAdminAdapter.OnOrderActionListener() {
            @Override public void onConfirm(String orderId) { confirmOrder(orderId); }
            @Override public void onCancel(String orderId)  { askCancelOrder(orderId); }
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);

        btnTabPending.setOnClickListener(v  -> switchTab(TAB_PENDING));
        btnTabConfirmed.setOnClickListener(v-> switchTab(TAB_CONFIRMED));
        btnTabAll.setOnClickListener(v      -> switchTab(TAB_ALL));

        applyTabStyle(TAB_PENDING);
        loadOrders();
        return view;
    }

    private void switchTab(int tab) {
        currentTab = tab;
        applyTabStyle(tab);
        loadOrders();
    }

    private void applyTabStyle(int active) {
        int activeColor  = 0xFFC62828;
        int activeText   = 0xFFFFFFFF;
        int inactiveColor= 0xFFFFCDD2;
        int inactiveText = 0xFFC62828;

        int[] colors = {inactiveColor, inactiveColor, inactiveColor};
        int[] texts  = {inactiveText,  inactiveText,  inactiveText};
        colors[active] = activeColor;
        texts[active]  = activeText;

        btnTabPending.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[0]));
        btnTabPending.setTextColor(texts[0]);
        btnTabConfirmed.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[1]));
        btnTabConfirmed.setTextColor(texts[1]);
        btnTabAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colors[2]));
        btnTabAll.setTextColor(texts[2]);
    }

    private void loadOrders() {
        com.google.firebase.firestore.Query query = db.collection("orders");
        if (currentTab == TAB_PENDING) {
            query = query.whereEqualTo("status", "pending");
        } else if (currentTab == TAB_CONFIRMED) {
            query = query.whereEqualTo("status", "confirmed");
        }
        query.get().addOnSuccessListener(this::processDocs);
    }

    private void processDocs(QuerySnapshot snapshots) {
        if (!isAdded()) return;
        orderList.clear();
        orderIds.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            orderList.add(doc.getData());
            orderIds.add(doc.getId());
        }
        adapter.updateList(orderList, orderIds);
        String[] labels = {"chờ xác nhận", "đã xác nhận", "tổng đơn"};
        tvCount.setText(orderList.size() + " " + labels[currentTab]);
        tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(orderList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void confirmOrder(String orderId) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "confirmed");
        db.collection("orders").document(orderId).update(update)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "✅ Đã xác nhận đơn hàng", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void askCancelOrder(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này không?\nHành động này sẽ trả xe về trạng thái đang bán.")
                .setPositiveButton("Hủy đơn", (d, w) -> cancelOrder(orderId))
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrder(String orderId) {
        // Lấy carId trong đơn để reset trạng thái xe về active
        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(doc -> {
                    String carId = doc.getString("carId");
                    Map<String, Object> orderUpdate = new HashMap<>();
                    orderUpdate.put("status", "cancelled");
                    db.collection("orders").document(orderId).update(orderUpdate);

                    if (carId != null && !carId.isEmpty()) {
                        Map<String, Object> carUpdate = new HashMap<>();
                        carUpdate.put("status", "active");
                        db.collection("cars").document(carId).update(carUpdate);
                    }

                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Đã hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    loadOrders();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }
}
