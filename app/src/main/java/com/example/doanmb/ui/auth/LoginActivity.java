package com.example.doanmb.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.MainActivity;
import com.example.doanmb.data.repository.AuthRepository;
import com.example.doanmb.databinding.ActivityLoginBinding;
import com.example.doanmb.ui.admin.AdminDashboardActivity;
import com.example.doanmb.ui.driver.DriverDashboardActivity;

/** Màn đăng nhập — MVVM (email hoặc số điện thoại). */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đăng nhập");
        }

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        binding.btnDoLogin.setOnClickListener(v ->
                viewModel.login(binding.etEmail.getText().toString().trim(),
                        binding.etPassword.getText().toString().trim()));
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
        binding.tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPassActivity.class)));

        viewModel.getMessage().observe(this, m -> {
            if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
        });
        viewModel.getRoute().observe(this, this::navigate);
    }

    private void navigate(AuthRepository.Route route) {
        Intent intent;
        if (route == AuthRepository.Route.ADMIN) intent = new Intent(this, AdminDashboardActivity.class);
        else if (route == AuthRepository.Route.DRIVER) intent = new Intent(this, DriverDashboardActivity.class);
        else intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
