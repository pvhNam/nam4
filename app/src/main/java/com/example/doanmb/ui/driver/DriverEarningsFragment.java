package com.example.doanmb.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.databinding.FragmentDriverEarningsBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tab "Thu nhập" tài xế (driver4) — MVVM: dữ liệu ở {@link DriverEarningsViewModel};
 * Fragment chỉ tính phần hiển thị theo kỳ & vẽ biểu đồ.
 */
public class DriverEarningsFragment extends BaseFragment {

    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    private FragmentDriverEarningsBinding binding;
    private DriverEarningsViewModel viewModel;
    private int currentPeriod = 0;
    private List<double[]> completed = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverEarningsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverEarningsViewModel.class);

        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Hôm nay"));
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Tuần này"));
        binding.tabPeriod.addTab(binding.tabPeriod.newTab().setText("Tháng này"));
        binding.tabPeriod.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { currentPeriod = t.getPosition(); refreshPeriod(); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });

        binding.btnWithdraw.setOnClickListener(x -> toast("Rút tiền: liên hệ admin để duyệt."));
        binding.btnTxnHistory.setOnClickListener(x -> toast("Lịch sử giao dịch đang được phát triển."));

        setupChart();

        viewModel.getName().observe(getViewLifecycleOwner(), n -> binding.driverHeader.tvDhName.setText(n));
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) Glide.with(this).load(url).into(binding.driverHeader.ivDhAvatar);
        });
        viewModel.getBalance().observe(getViewLifecycleOwner(), b ->
                binding.tvEBalance.setText(MONEY.format(b != null ? b : 0L) + "đ"));
        viewModel.getCompleted().observe(getViewLifecycleOwner(), list -> {
            completed = list != null ? list : new ArrayList<>();
            refreshPeriod();
            refreshChart();
            refreshReward();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.load();
    }

    private void refreshPeriod() {
        long start = periodStart(currentPeriod);
        double revenue = 0;
        int trips = 0;
        for (double[] t : completed) if (t[0] >= start) { revenue += t[1]; trips++; }
        binding.tvERevenue.setText(MONEY.format(revenue) + "đ");
        binding.tvETrips.setText(String.valueOf(trips));
    }

    private long periodStart(int period) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (period == 1) c.add(Calendar.DAY_OF_MONTH, -6);
        else if (period == 2) c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    private void refreshReward() {
        long start = periodStart(1);
        int weekTrips = 0;
        for (double[] t : completed) if (t[0] >= start) weekTrips++;
        binding.progressReward.setProgress(Math.min(weekTrips, 10));
        binding.tvRewardProgress.setText(weekTrips + " / 10 chuyến");
    }

    private void setupChart() {
        binding.chartIncome.getDescription().setEnabled(false);
        binding.chartIncome.getLegend().setEnabled(false);
        binding.chartIncome.setScaleEnabled(false);
        binding.chartIncome.setDragEnabled(false);
        binding.chartIncome.setPinchZoom(false);
        binding.chartIncome.setDrawGridBackground(false);
        binding.chartIncome.setDrawBorders(false);
        binding.chartIncome.setExtraBottomOffset(6f);

        XAxis x = binding.chartIncome.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setDrawAxisLine(false);
        x.setGranularity(1f);
        x.setTextColor(0xFF9AA0A6);

        YAxis left = binding.chartIncome.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setDrawAxisLine(false);
        left.setGridColor(0xFFEEF1F5);
        left.setTextColor(0xFF9AA0A6);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                if (value >= 1000) return (int) (value / 1000) + "k";
                return String.valueOf((int) value);
            }
        });
        binding.chartIncome.getAxisRight().setEnabled(false);
    }

    private void refreshChart() {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM", Locale.getDefault());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -i);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            long dayStart = c.getTimeInMillis();
            long dayEnd = dayStart + 24L * 60 * 60 * 1000;

            double sum = 0;
            for (double[] t : completed) if (t[0] >= dayStart && t[0] < dayEnd) sum += t[1];

            int idx = 5 - i;
            entries.add(new BarEntry(idx, (float) sum));
            labels.add(fmt.format(new Date(dayStart)));
            colors.add(i == 0 ? 0xFF2E6BF0 : 0xFFBBD2F7);
        }

        BarDataSet set = new BarDataSet(entries, "Thu nhập");
        set.setColors(colors);
        set.setDrawValues(false);

        BarData data = new BarData(set);
        data.setBarWidth(0.5f);
        binding.chartIncome.setData(data);
        binding.chartIncome.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.chartIncome.setFitBars(true);
        binding.chartIncome.animateY(600);
        binding.chartIncome.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
