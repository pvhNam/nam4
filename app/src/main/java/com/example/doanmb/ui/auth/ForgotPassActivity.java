package com.example.doanmb.ui.auth;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.databinding.ActivityForgotpassBinding;

/** Màn quên mật khẩu — MVVM. */
public class ForgotPassActivity extends AppCompatActivity {

    private ActivityForgotpassBinding binding;
    private ForgotPassViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotpassBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ForgotPassViewModel.class);

        binding.btnReset.setOnClickListener(v ->
                viewModel.reset(binding.etEmail.getText().toString().trim()));

        viewModel.getMessage().observe(this, m -> {
            if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
        });
        viewModel.getSuccess().observe(this, ok -> { if (Boolean.TRUE.equals(ok)) finish(); });
    }
}
