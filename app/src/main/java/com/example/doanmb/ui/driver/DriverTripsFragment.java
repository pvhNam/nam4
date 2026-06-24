package com.example.doanmb.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.doanmb.adapter.TripAdapter;
import com.example.doanmb.databinding.FragmentDriverTripsBinding;
import com.example.doanmb.model.Trip;
import com.example.doanmb.ui.base.BaseFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/** Tab "Chuyến" tài xế (driver2) — MVVM. */
public class DriverTripsFragment extends BaseFragment implements TripAdapter.OnTripActionListener {

    private FragmentDriverTripsBinding binding;
    private DriverTripsViewModel viewModel;
    private TripAdapter adapter;
    private final List<Trip> shown = new ArrayList<>();
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverTripsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverTripsViewModel.class);

        adapter = new TripAdapter(shown, this);
        binding.rvTrips.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTrips.setAdapter(adapter);

        binding.tabTrips.addTab(binding.tabTrips.newTab().setText("Chuyến mới"));
        binding.tabTrips.addTab(binding.tabTrips.newTab().setText("Đang chạy"));
        binding.tabTrips.addTab(binding.tabTrips.newTab().setText("Lịch sử"));
        binding.tabTrips.addTab(binding.tabTrips.newTab().setText("Đặt trước"));
        binding.tabTrips.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { currentTab = tab.getPosition(); renderTab(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.rowRunning.setOnClickListener(x -> selectTab(1));
        binding.rowHistory.setOnClickListener(x -> selectTab(2));
        binding.rowScheduled.setOnClickListener(x -> selectTab(3));

        viewModel.getName().observe(getViewLifecycleOwner(), n -> binding.driverHeader.tvDhName.setText(n));
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) Glide.with(this).load(url).into(binding.driverHeader.ivDhAvatar);
        });
        viewModel.getWaiting().observe(getViewLifecycleOwner(), l -> { if (currentTab == 0) renderTab(); });
        viewModel.getRunning().observe(getViewLifecycleOwner(), l -> { if (currentTab == 1) renderTab(); });
        viewModel.getHistory().observe(getViewLifecycleOwner(), l -> { if (currentTab == 2) renderTab(); });
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.load();
    }

    private void selectTab(int i) {
        TabLayout.Tab tab = binding.tabTrips.getTabAt(i);
        if (tab != null) tab.select();
    }

    private List<Trip> listForTab() {
        switch (currentTab) {
            case 0: return orEmpty(viewModel.getWaiting().getValue());
            case 1: return orEmpty(viewModel.getRunning().getValue());
            case 2: return orEmpty(viewModel.getHistory().getValue());
            default: return new ArrayList<>();
        }
    }

    private List<Trip> orEmpty(List<Trip> l) { return l != null ? l : new ArrayList<>(); }

    private void renderTab() {
        shown.clear();
        shown.addAll(listForTab());
        switch (currentTab) {
            case 0:
                binding.tvSectionTitle.setText("Yêu cầu mới");
                binding.tvCountdown.setVisibility(View.VISIBLE);
                binding.tvEmpty.setText("Chưa có yêu cầu mới");
                break;
            case 1:
                binding.tvSectionTitle.setText("Đang chạy");
                binding.tvCountdown.setVisibility(View.GONE);
                binding.tvEmpty.setText("Không có chuyến đang chạy");
                break;
            case 2:
                binding.tvSectionTitle.setText("Lịch sử chuyến");
                binding.tvCountdown.setVisibility(View.GONE);
                binding.tvEmpty.setText("Chưa có chuyến hoàn thành");
                break;
            default:
                binding.tvSectionTitle.setText("Chuyến đặt trước");
                binding.tvCountdown.setVisibility(View.GONE);
                binding.tvEmpty.setText("Chưa có chuyến đặt trước");
        }
        adapter.notifyDataSetChanged();
        binding.tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPrimary(Trip trip) {
        if (trip.getId() == null) return;
        if (Trip.STATUS_WAITING.equals(trip.getStatus())) { viewModel.accept(trip); selectTab(1); }
        else if (Trip.STATUS_RUNNING.equals(trip.getStatus())) viewModel.complete(trip);
    }

    @Override
    public void onSkip(Trip trip) {
        viewModel.skip(trip);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
