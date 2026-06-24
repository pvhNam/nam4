package com.example.doanmb.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.MainActivity;
import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.databinding.ActivitySplashBinding;
import com.example.doanmb.ui.admin.AdminDashboardActivity;
import com.example.doanmb.ui.driver.DriverDashboardActivity;

/** Màn khởi động — MVVM: kiểm tra phiên & điều hướng theo vai trò. */
public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private SplashViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().hide();
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        viewModel.getRoute().observe(this, this::navigate);

        if (viewModel.hasSession()) {
            binding.btnNext.setVisibility(View.GONE);
            new Handler(Looper.getMainLooper()).postDelayed(() -> viewModel.resolveRoute(), 1500);
        } else {
            binding.btnNext.setVisibility(View.VISIBLE);
            binding.btnNext.setOnClickListener(v -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        }
    }

    private void navigate(AuthRepository.Route route) {
        Intent intent;
        if (route == AuthRepository.Route.ADMIN) intent = new Intent(this, AdminDashboardActivity.class);
        else if (route == AuthRepository.Route.DRIVER) intent = new Intent(this, DriverDashboardActivity.class);
        else intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
