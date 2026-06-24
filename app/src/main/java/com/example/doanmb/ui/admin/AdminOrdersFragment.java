package com.example.doanmb.ui.admin;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.adapter.OrderAdminAdapter;
import com.example.doanmb.databinding.FragmentAdminOrdersBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.example.doanmb.ui.admin.OrderViewModel;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Màn quản lý đơn của Admin — đã chuyển sang MVVM: Fragment chỉ lo hiển thị và
 * chuyển sự kiện, mọi nghiệp vụ nằm ở {@link OrderViewModel}.
 */
public class AdminOrdersFragment extends BaseFragment {

    private FragmentAdminOrdersBinding binding;
    private OrderViewModel viewModel;
    private OrderAdminAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OrderViewModel.class);

        adapter = new OrderAdminAdapter(new ArrayList<>(), new ArrayList<>(),
                new OrderAdminAdapter.OnOrderActionListener() {
                    @Override public void onConfirm(String orderId)  { viewModel.confirmOrder(orderId); }
                    @Override public void onComplete(String orderId) { askComplete(orderId); }
                    @Override public void onCancel(String orderId)   { askCancel(orderId); }
                });
        binding.rvAdminOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAdminOrders.setAdapter(adapter);

        binding.btnOrderTabPending.setOnClickListener(v   -> switchTab(OrderViewModel.TAB_PENDING));
        binding.btnOrderTabConfirmed.setOnClickListener(v -> switchTab(OrderViewModel.TAB_CONFIRMED));
        binding.btnOrderTabAll.setOnClickListener(v       -> switchTab(OrderViewModel.TAB_ALL));

        observe();
        switchTab(OrderViewModel.TAB_PENDING);
    }

    private void observe() {
        viewModel.getOrders().observe(getViewLifecycleOwner(), this::renderOrders);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
    }

    private void switchTab(int tab) {
        applyTabStyle(tab);
        viewModel.selectTab(tab);
    }

    private void renderOrders(List<DocumentSnapshot> docs) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (docs != null) {
            for (DocumentSnapshot d : docs) { data.add(d.getData()); ids.add(d.getId()); }
        }
        adapter.updateList(data, ids);

        String[] labels = {"chờ xác nhận", "đã xác nhận", "tổng đơn"};
        binding.tvAdminOrderCount.setText(data.size() + " " + labels[viewModel.getCurrentTab()]);
        boolean empty = data.isEmpty();
        binding.tvAdminEmptyOrders.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvAdminOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void applyTabStyle(int active) {
        int activeColor = 0xFFC62828, activeText = 0xFFFFFFFF;
        int inColor = 0xFFFFCDD2,    inText = 0xFFC62828;
        int[] colors = {inColor, inColor, inColor};
        int[] texts  = {inText,  inText,  inText};
        colors[active] = activeColor; texts[active] = activeText;
        binding.btnOrderTabPending.setBackgroundTintList(ColorStateList.valueOf(colors[0]));
        binding.btnOrderTabPending.setTextColor(texts[0]);
        binding.btnOrderTabConfirmed.setBackgroundTintList(ColorStateList.valueOf(colors[1]));
        binding.btnOrderTabConfirmed.setTextColor(texts[1]);
        binding.btnOrderTabAll.setBackgroundTintList(ColorStateList.valueOf(colors[2]));
        binding.btnOrderTabAll.setTextColor(texts[2]);
    }

    private void askComplete(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hoàn thành đơn")
                .setMessage("Xác nhận đơn đã hoàn thành?\nApp sẽ trừ 15% hoa hồng từ tiền cọc và trả 85% còn lại về ví chủ xe/tài xế.")
                .setPositiveButton("Hoàn thành", (d, w) -> viewModel.completeOrder(orderId))
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void askCancel(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc muốn hủy đơn hàng này không?\nHành động này sẽ trả xe về trạng thái đang bán.")
                .setPositiveButton("Hủy đơn", (d, w) -> viewModel.cancelOrder(orderId))
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadOrders();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
