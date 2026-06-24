package com.example.doanmb.ui.driver;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.databinding.FragmentDriverMapBinding;
import com.example.doanmb.ui.base.BaseFragment;

/** Tab "Bản đồ" tài xế (driver3) — MVVM. Bản đồ là ảnh tĩnh (không cần API key). */
public class DriverMapFragment extends BaseFragment {

    private FragmentDriverMapBinding binding;
    private DriverMapViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverMapViewModel.class);

        binding.switchReceive.setOnClickListener(x -> viewModel.setOnline(binding.switchReceive.isChecked()));
        binding.btnToggleReceive.setOnClickListener(x -> viewModel.setOnline(!viewModel.isOnline()));
        binding.cardNavigate.setOnClickListener(x -> openMaps());
        binding.cardMoveHistory.setOnClickListener(x -> toast("Lịch sử di chuyển đang được phát triển"));

        viewModel.getName().observe(getViewLifecycleOwner(), n -> binding.driverHeader.tvDhName.setText(n));
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) Glide.with(this).load(url).into(binding.driverHeader.ivDhAvatar);
        });
        viewModel.getOnline().observe(getViewLifecycleOwner(), this::applyOnlineUi);
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

    private void openMaps() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=trạm xăng"));
            intent.setPackage("com.google.android.apps.maps");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps")));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
