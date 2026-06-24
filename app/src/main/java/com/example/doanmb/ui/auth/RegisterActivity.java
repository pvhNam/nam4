package com.example.doanmb.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.databinding.ActivityRegisterBinding;

/** Màn đăng ký tài khoản — MVVM. */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        binding.btnDoRegister.setOnClickListener(v -> viewModel.register(
                binding.etRegisterName.getText().toString().trim(),
                binding.etRegisterPhone.getText().toString().trim(),
                binding.etRegisterEmail.getText().toString().trim(),
                binding.etRegisterPassword.getText().toString().trim(),
                binding.etRegisterConfirmPassword.getText().toString().trim()));
        binding.tvBackToLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));

        viewModel.getMessage().observe(this, m -> {
            if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
        });
        viewModel.getSuccess().observe(this, ok -> { if (Boolean.TRUE.equals(ok)) finish(); });
    }
}
