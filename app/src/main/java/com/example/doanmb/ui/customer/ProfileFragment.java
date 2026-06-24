package com.example.doanmb.ui.customer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.databinding.FragmentProfileBinding;
import com.example.doanmb.ui.activity.AboutUsActivity;
import com.example.doanmb.ui.auth.LoginActivity;
import com.example.doanmb.ui.auth.RegisterActivity;
import com.example.doanmb.ui.base.BaseFragment;
import com.example.doanmb.ui.driver.DriverDashboardActivity;
import com.example.doanmb.ui.driver.DriverRegisterActivity;
import com.example.doanmb.util.ImageLoader;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/** Trang cá nhân khách (3 lớp con: hồ sơ / cài đặt / thông tin cá nhân) — MVVM. */
public class ProfileFragment extends BaseFragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private String driverStatus = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) viewModel.uploadAvatar(requireContext().getApplicationContext(), uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        setupListeners();
        observe();
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        binding.btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));
        binding.btnLogout.setOnClickListener(v -> viewModel.logout());

        binding.cardUserProfile.setOnClickListener(v -> switchSubScreen(2));
        binding.btnBackToMain.setOnClickListener(v -> switchSubScreen(1));
        binding.menuPersonalInfo.setOnClickListener(v -> switchSubScreen(3));
        binding.menuFavoriteCars.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), FavoriteCarsActivity.class)));
        binding.menuRegisterDriver.setOnClickListener(v -> {
            if ("approved".equals(driverStatus)) openDriverDashboard();
            else startActivity(new Intent(getActivity(), DriverRegisterActivity.class));
        });
        binding.btnSwitchDriver.setOnClickListener(v -> openDriverDashboard());
        binding.btnBackToSettings.setOnClickListener(v -> switchSubScreen(2));
        binding.ivChangeAvatarTrigger.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.btnSavePersonalInfo.setOnClickListener(v -> viewModel.saveInfo(
                binding.edtInfoName.getText().toString().trim(),
                binding.edtInfoDob.getText().toString().trim(),
                binding.edtInfoGender.getText().toString().trim(),
                binding.edtInfoPhone.getText().toString().trim()));
        binding.edtInfoDob.setOnClickListener(v -> showDatePicker());
        binding.menuAboutUs.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUsActivity.class)));
    }

    private void observe() {
        viewModel.getLoggedIn().observe(getViewLifecycleOwner(), logged -> {
            boolean in = Boolean.TRUE.equals(logged);
            binding.layoutNotLoggedIn.setVisibility(in ? View.GONE : View.VISIBLE);
            binding.layoutLoggedIn.setVisibility(in ? View.VISIBLE : View.GONE);
        });
        viewModel.getProfile().observe(getViewLifecycleOwner(), this::renderProfile);
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null && !url.isEmpty()) {
                ImageLoader.loadAvatar(binding.ivAvatarMain, url);
                ImageLoader.loadAvatar(binding.ivAvatarSettings, url);
            }
        });
        viewModel.getInfoSaved().observe(getViewLifecycleOwner(), ok -> { if (Boolean.TRUE.equals(ok)) switchSubScreen(2); });
        viewModel.getLoggedOut().observe(getViewLifecycleOwner(), ok -> {
            if (Boolean.TRUE.equals(ok)) { switchSubScreen(1); viewModel.refresh(); }
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.refresh();
    }

    private void renderProfile(ProfileViewModel.UserProfile p) {
        if (p == null) return;
        driverStatus = p.driverStatus;

        binding.btnSwitchDriver.setVisibility("approved".equals(driverStatus) ? View.VISIBLE : View.GONE);
        binding.tvRegisterDriverLabel.setText("approved".equals(driverStatus)
                ? "Chuyển sang chế độ tài xế" : "Đăng ký làm tài xế");

        if ("pending".equals(driverStatus)) {
            binding.tvDriverStatusHint.setText("Chờ duyệt");
            binding.tvDriverStatusHint.setTextSize(13);
            binding.tvDriverStatusHint.setTextColor(Color.parseColor("#EF6C00"));
        } else if ("approved".equals(driverStatus)) {
            binding.tvDriverStatusHint.setText("Đã duyệt");
            binding.tvDriverStatusHint.setTextSize(13);
            binding.tvDriverStatusHint.setTextColor(Color.parseColor("#2E7D32"));
        } else if ("rejected".equals(driverStatus)) {
            binding.tvDriverStatusHint.setText("Bị từ chối");
            binding.tvDriverStatusHint.setTextSize(13);
            binding.tvDriverStatusHint.setTextColor(Color.parseColor("#C62828"));
        } else {
            binding.tvDriverStatusHint.setText("›");
            binding.tvDriverStatusHint.setTextSize(24);
            binding.tvDriverStatusHint.setTextColor(Color.parseColor("#2A70DE"));
        }

        binding.tvProfileNameMain.setText(p.name != null ? p.name : "Chưa đặt tên");
        binding.tvWalletBalance.setText("💰 Số dư ví: "
                + NumberFormat.getInstance(new Locale("vi", "VN")).format(p.balance) + " đ");

        if (p.phoneVerified) {
            binding.tvPhoneVerifiedBadge.setText("Đã xác thực");
            binding.tvPhoneVerifiedBadge.setTextColor(Color.parseColor("#4CAF50"));
            binding.ivVerifiedIcon.setVisibility(View.VISIBLE);
        } else {
            binding.tvPhoneVerifiedBadge.setText("Chưa xác thực số điện thoại");
            binding.tvPhoneVerifiedBadge.setTextColor(Color.parseColor("#E53935"));
            binding.ivVerifiedIcon.setVisibility(View.GONE);
        }

        binding.edtInfoName.setText(p.name != null ? p.name : "");
        binding.edtInfoPhone.setText(p.phone != null ? p.phone : "");
        binding.edtInfoDob.setText(p.dob != null ? p.dob : "");
        binding.edtInfoGender.setText(p.gender != null ? p.gender : "Nam");
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(getContext(),
                (v, year, month, day) ->
                        binding.edtInfoDob.setText(String.format(Locale.US, "%02d/%02d/%04d", day, month + 1, year)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void switchSubScreen(int screenId) {
        binding.layoutMainProfile.setVisibility(screenId == 1 ? View.VISIBLE : View.GONE);
        binding.layoutAccountSettings.setVisibility(screenId == 2 ? View.VISIBLE : View.GONE);
        binding.layoutPersonalInfo.setVisibility(screenId == 3 ? View.VISIBLE : View.GONE);
    }

    private void openDriverDashboard() {
        startActivity(new Intent(getActivity(), DriverDashboardActivity.class));
        if (getActivity() != null) getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
