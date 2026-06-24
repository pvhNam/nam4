package com.example.doanmb.ui.driver;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.databinding.ActivityDriverRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Khách điền CCCD + bằng lái + loại xe để đăng ký làm tài xế — MVVM.
 * Gửi -> users.driverStatus = "pending" (chờ Admin duyệt).
 */
public class DriverRegisterActivity extends AppCompatActivity {

    private ActivityDriverRegisterBinding binding;
    private DriverRegisterViewModel viewModel;

    private int targetImage = 0; // 1 = CCCD, 2 = bằng lái
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) viewModel.uploadImage(getApplicationContext(), uri, targetImage);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        viewModel = new ViewModelProvider(this).get(DriverRegisterViewModel.class);

        binding.spinnerDrCartype.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"4 chỗ", "7 chỗ", "16 chỗ", "Xe tải"}));

        binding.btnDrBack.setOnClickListener(v -> finish());
        binding.cardDrCccdImg.setOnClickListener(v -> { targetImage = 1; pickImageLauncher.launch("image/*"); });
        binding.cardDrLicenseImg.setOnClickListener(v -> { targetImage = 2; pickImageLauncher.launch("image/*"); });
        binding.btnDrSubmit.setOnClickListener(v -> submit());

        observe();
        viewModel.load();
    }

    private void observe() {
        viewModel.getExisting().observe(this, e -> {
            if (e == null) return;
            if (e.cccd != null) binding.edtDrCccd.setText(e.cccd);
            if (e.license != null) binding.edtDrLicense.setText(e.license);
            String banner = null;
            if ("pending".equals(e.status)) {
                banner = "Hồ sơ của bạn đang chờ Admin duyệt.";
                binding.btnDrSubmit.setText("Cập nhật hồ sơ");
            } else if ("rejected".equals(e.status)) {
                banner = "Hồ sơ bị từ chối. Vui lòng cập nhật và gửi lại.";
            } else if ("approved".equals(e.status)) {
                banner = "Bạn đã là tài xế. Đăng xuất và đăng nhập lại để vào trang tài xế.";
            }
            if (banner != null) {
                binding.tvDrStatusBanner.setVisibility(View.VISIBLE);
                binding.tvDrStatusBanner.setText(banner);
            }
        });
        viewModel.getCccdImageUrl().observe(this, url -> {
            if (url != null) Glide.with(this).load(url).into(binding.ivDrCccdImg);
        });
        viewModel.getLicenseImageUrl().observe(this, url -> {
            if (url != null) Glide.with(this).load(url).into(binding.ivDrLicenseImg);
        });
        viewModel.getSubmitting().observe(this, s -> binding.btnDrSubmit.setEnabled(!Boolean.TRUE.equals(s)));
        viewModel.getMessage().observe(this, m -> {
            if (m != null) Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
        });
        viewModel.getFinished().observe(this, done -> { if (Boolean.TRUE.equals(done)) finish(); });
    }

    private void submit() {
        String cccd = binding.edtDrCccd.getText().toString().trim();
        String license = binding.edtDrLicense.getText().toString().trim();
        String carType = binding.spinnerDrCartype.getSelectedItem() != null
                ? binding.spinnerDrCartype.getSelectedItem().toString() : "";
        viewModel.submit(cccd, license, carType);
    }
}
