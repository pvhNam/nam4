package com.example.doanmb.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.databinding.FragmentAdminDriverDetailBinding;
import com.example.doanmb.model.User;
import com.example.doanmb.ui.base.BaseFragment;

import java.text.SimpleDateFormat;
import java.util.Locale;

/** Màn chi tiết & duyệt hồ sơ tài xế — MVVM. */
public class AdminDriverDetailFragment extends BaseFragment {

    private static final String ARG_UID = "uid";
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("HH:mm  dd/MM/yyyy", Locale.getDefault());

    private FragmentAdminDriverDetailBinding binding;
    private AdminDriverDetailViewModel viewModel;
    private String uid;

    public static AdminDriverDetailFragment newInstance(String uid) {
        AdminDriverDetailFragment f = new AdminDriverDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID, uid);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uid = getArguments() != null ? getArguments().getString(ARG_UID) : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDriverDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminDriverDetailViewModel.class);

        binding.btnDdBack.setOnClickListener(v -> popBack());
        binding.btnDdApprove.setOnClickListener(v -> { setButtons(false); viewModel.approve(uid); });
        binding.btnDdReject.setOnClickListener(v -> { setButtons(false); viewModel.reject(uid); });

        viewModel.getUser().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
        viewModel.getFinished().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) popBack();
            else setButtons(true);
        });

        viewModel.load(uid);
    }

    private void render(User user) {
        if (user == null) return;
        binding.tvDdName.setText(safe(user.getName(), "--"));
        binding.tvDdPhone.setText("SĐT: " + safe(user.getPhone(), "--"));
        binding.tvDdAppliedAt.setText(user.getAppliedAt() != null
                ? "Gửi lúc: " + SDF.format(user.getAppliedAt().toDate()) : "Gửi lúc: --");
        binding.tvDdCccd.setText("Số CCCD: " + safe(user.getCccd(), "--"));
        binding.tvDdLicense.setText("Số bằng lái: " + safe(user.getLicenseNumber(), "--"));
        binding.tvDdCartype.setText("Loại xe: " + safe(user.getDriverCarType(), "--"));
        loadImg(binding.ivDdCccd, user.getCccdImageUrl());
        loadImg(binding.ivDdLicense, user.getLicenseImageUrl());
    }

    private void setButtons(boolean enabled) {
        if (binding == null) return;
        binding.btnDdApprove.setEnabled(enabled);
        binding.btnDdReject.setEnabled(enabled);
    }

    private void popBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void loadImg(ImageView iv, String url) {
        if (url != null && !url.isEmpty()) Glide.with(this).load(url).into(iv);
    }

    private String safe(String val, String def) {
        return val != null && !val.isEmpty() ? val : def;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
