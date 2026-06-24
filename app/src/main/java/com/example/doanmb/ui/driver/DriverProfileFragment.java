package com.example.doanmb.ui.driver;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.MainActivity;
import com.example.doanmb.databinding.FragmentDriverProfileBinding;
import com.example.doanmb.ui.auth.LoginActivity;
import com.example.doanmb.ui.base.BaseFragment;
import com.example.doanmb.ui.customer.FavoriteCarsActivity;
import com.google.firebase.auth.FirebaseAuth;

/** Tab "Cá nhân" của tài xế (driver5) — MVVM. */
public class DriverProfileFragment extends BaseFragment {

    private FragmentDriverProfileBinding binding;
    private DriverProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverProfileViewModel.class);

        binding.rowRegister.setOnClickListener(x -> {
            if (getActivity() instanceof DriverDashboardActivity) {
                ((DriverDashboardActivity) getActivity()).openPostForm();
            }
        });
        binding.rowCustomerMode.setOnClickListener(x -> switchToUserMode());
        binding.rowFavorites.setOnClickListener(x ->
                startActivity(new Intent(getActivity(), FavoriteCarsActivity.class)));

        View.OnClickListener soon = x -> toast("Tính năng đang được phát triển");
        binding.rowLocation.setOnClickListener(soon);
        binding.rowReviews.setOnClickListener(soon);
        binding.rowGifts.setOnClickListener(soon);
        binding.rowRefer.setOnClickListener(soon);
        binding.rowPrivacy.setOnClickListener(soon);
        binding.rowSupport.setOnClickListener(soon);
        binding.cardProfile.setOnClickListener(soon);

        binding.btnDpLogout.setOnClickListener(x -> logout());

        viewModel.getName().observe(getViewLifecycleOwner(), name -> binding.tvDpName.setText(name));
        viewModel.getAvatarUrl().observe(getViewLifecycleOwner(), url -> {
            if (url != null) Glide.with(this).load(url).into(binding.ivDpAvatar);
        });

        viewModel.load();
    }

    private void switchToUserMode() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
