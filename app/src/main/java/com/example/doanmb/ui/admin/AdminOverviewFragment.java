package com.example.doanmb.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.R;
import com.example.doanmb.databinding.FragmentAdminOverviewBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.List;

/** Màn Tổng quan Admin — MVVM: Fragment chỉ vẽ; số liệu ở {@link AdminOverviewViewModel}. */
public class AdminOverviewFragment extends BaseFragment {

    /** Listener cho điều hướng nhanh tới các tab khác (Activity host implement). */
    public interface OnQuickNavListener {
        void navigateTo(int itemId);
    }

    private FragmentAdminOverviewBinding binding;
    private AdminOverviewViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminOverviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminOverviewViewModel.class);

        setupChart();

        binding.tvViewRevenueDetail.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AdminRevenueDetailActivity.class)));
        binding.btnViewUsers.setOnClickListener(v -> navigate(R.id.nav_admin_users));
        binding.btnViewCars.setOnClickListener(v -> navigate(R.id.nav_admin_cars));
        binding.btnViewOrders.setOnClickListener(v -> navigate(R.id.nav_admin_orders));
        binding.btnViewDriverApproval.setOnClickListener(v -> navigate(R.id.nav_admin_driver_approval));

        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        viewModel.getChart().observe(getViewLifecycleOwner(), this::renderChart);
        viewModel.getDriverPending().observe(getViewLifecycleOwner(), this::renderDriverPending);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);

        viewModel.load();
    }

    private void navigate(int itemId) {
        if (getActivity() instanceof OnQuickNavListener) {
            ((OnQuickNavListener) getActivity()).navigateTo(itemId);
        }
    }

    private void renderSummary(AdminOverviewViewModel.Summary s) {
        if (s == null) return;
        binding.tvConfirmedCount.setText(String.valueOf(s.confirmedCount));
        binding.tvSaleRevenue.setText(formatRevenue(s.sale));
        binding.tvRentalRevenue.setText(formatRevenue(s.rental));
        binding.tvTotalRevenue.setText(formatRevenue(s.total));
        binding.tvMonthRevenue.setText(formatRevenue(s.month));
        binding.tvPostingFeeRevenue.setText(formatRevenue(s.postingFee));
        binding.tvPostingCarCount.setText(s.carCount + " bài đăng × 200.000 VNĐ");
    }

    private void renderDriverPending(Integer count) {
        if (count == null) return;
        if (count == 0) {
            binding.tvDriverPendingCount.setText("Không có hồ sơ đang chờ");
        } else {
            binding.tvDriverPendingCount.setText(count + " hồ sơ đang chờ duyệt");
            binding.tvDriverPendingCount.setTextColor(0xFFC62828);
        }
    }

    private String formatRevenue(long amount) {
        if (amount == 0) return "0 VNĐ";
        if (amount >= 1_000_000_000L) return String.format("%.1f tỷ", amount / 1_000_000_000.0);
        if (amount >= 1_000_000L) return String.format("%.0f triệu", amount / 1_000_000.0);
        return (amount / 1_000) + "K VNĐ";
    }

    private void setupChart() {
        BarChart barChart = binding.barChartRevenue;
        barChart.setDrawGridBackground(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setTouchEnabled(false);
        barChart.setExtraBottomOffset(8f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(0xFF757575);
        xAxis.setTextSize(11f);

        YAxis left = barChart.getAxisLeft();
        left.setDrawGridLines(true);
        left.setGridColor(0xFFEEEEEE);
        left.setTextColor(0xFF757575);
        left.setTextSize(10f);
        left.setAxisMinimum(0f);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                if (value >= 1_000f) return String.format("%.0ftr", value / 1_000f);
                return String.format("%.0f", value);
            }
        });
        barChart.getAxisRight().setEnabled(false);
    }

    private void renderChart(AdminOverviewViewModel.ChartData cd) {
        if (cd == null) return;
        BarChart barChart = binding.barChartRevenue;
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) entries.add(new BarEntry(i, cd.monthRevenue[i] / 1_000f));

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu");
        dataSet.setColor(0xFFC62828);
        dataSet.setValueTextColor(0xFF212121);
        dataSet.setValueTextSize(9f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                if (value == 0) return "";
                if (value >= 1_000f) return String.format("%.1ftr", value / 1_000f);
                return String.format("%.0fK", value);
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.55f);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(cd.labels));
        barChart.getXAxis().setLabelCount(6);
        barChart.setData(barData);
        barChart.animateY(600);
        barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
