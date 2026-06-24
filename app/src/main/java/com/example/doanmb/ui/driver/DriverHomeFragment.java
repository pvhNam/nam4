package com.example.doanmb.ui.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.databinding.FragmentDriverHomeBinding;
import com.example.doanmb.databinding.ItemDriverOverviewStatBinding;
import com.example.doanmb.model.Trip;
import com.example.doanmb.ui.base.BaseFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/** Trang chủ tài xế (driver1) — MVVM. */
public class DriverHomeFragment extends BaseFragment {

    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    private FragmentDriverHomeBinding binding;
    private DriverHomeViewModel viewModel;
    private List<Trip> waiting = new ArrayList<>();
    private int index = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverHomeViewModel.class);

        bindStat(binding.statRevenue, R.drawable.ic_money, R.color.driver_primary, "0đ", "Doanh thu");
        bindStat(binding.statTrips, R.drawable.ic_dnav_trips, R.color.driver_primary, "0", "Chuyến");
        bindStat(binding.statRating, R.drawable.ic_star, R.color.driver_orange, "4.9", "Đánh giá");
        bindStat(binding.statOnline, R.drawable.ic_clock, R.color.driver_primary, "8h 30m", "Online");

        binding.ivHomeAvatar.setOnClickListener(view -> {
            if (getActivity() instanceof DriverDashboardActivity) {
                ((DriverDashboardActivity) getActivity()).openProfileTab();
            }
        });
        binding.switchReceive.setOnClickListener(view -> viewModel.setOnline(binding.switchReceive.isChecked()));
        binding.btnToggleReceive.setOnClickListener(view -> viewModel.setOnline(!viewModel.isOnline()));
        binding.btnNSkip.setOnClickListener(view -> { index++; showNearest(); });
        binding.btnNAccept.setOnClickListener(view -> acceptCurrent());

        viewModel.getName().observe(getViewLifecycleOwner(), n -> binding.tvHomeName.setText(n));
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) Glide.with(this).load(url).into(binding.ivHomeAvatar);
        });
        viewModel.getOnline().observe(getViewLifecycleOwner(), this::applyOnlineUi);
        viewModel.getWaiting().observe(getViewLifecycleOwner(), list -> {
            waiting = list != null ? list : new ArrayList<>();
            index = 0;
            showNearest();
        });
        viewModel.getTodayStats().observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;
            binding.statRevenue.tvStatValue.setText(MONEY.format(s.revenue) + "đ");
            binding.statTrips.tvStatValue.setText(String.valueOf(s.trips));
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.load();
    }

    private void applyOnlineUi(boolean online) {
        binding.switchReceive.setChecked(online);
        binding.tvReceiveState.setText(online ? "Đang nhận chuyến" : "Đã tắt nhận chuyến");
        binding.btnToggleReceive.setText(online ? "Tắt nhận chuyến" : "Bật nhận chuyến");
    }

    private void showNearest() {
        boolean has = viewModel.isOnline() && index >= 0 && index < waiting.size();
        binding.cardNearest.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.tvNoNearest.setVisibility(has ? View.GONE : View.VISIBLE);
        if (!has) {
            binding.tvNoNearest.setText(viewModel.isOnline()
                    ? "Chưa có chuyến nào đang chờ"
                    : "Bạn đang offline — bật nhận chuyến để xem chuyến mới");
            return;
        }
        Trip t = waiting.get(index);
        binding.tvNCartype.setText(t.getCarType() != null ? t.getCarType() : "Chuyến xe");
        binding.tvNPickup.setText(safe(t.getPickup()));
        if (Trip.MODE_DISTANCE.equals(t.getRentMode())) {
            binding.tvNDest.setText(safe(t.getDestination()));
            binding.tvNMeta.setText(t.getDistanceKm() > 0 ? t.getDistanceKm() + " km" : "Theo quãng đường");
        } else {
            String unit = Trip.MODE_MONTH.equals(t.getRentMode()) ? "tháng" : "ngày";
            binding.tvNDest.setText("Thuê " + t.getDuration() + " " + unit);
            binding.tvNMeta.setText(t.rentModeLabel());
        }
        binding.tvNPrice.setText(MONEY.format(t.getPrice()) + "đ");
    }

    private void acceptCurrent() {
        if (index < 0 || index >= waiting.size()) return;
        viewModel.accept(waiting.get(index));
    }

    private void bindStat(ItemDriverOverviewStatBinding tile, int icon, int iconColor, String value, String label) {
        tile.ivStat.setImageResource(icon);
        tile.ivStat.setColorFilter(ContextCompat.getColor(requireContext(), iconColor));
        tile.tvStatValue.setText(value);
        tile.tvStatLabel.setText(label);
    }

    private String safe(String s) { return s != null && !s.isEmpty() ? s : "--"; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
