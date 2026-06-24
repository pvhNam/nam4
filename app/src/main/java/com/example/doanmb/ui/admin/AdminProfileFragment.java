package com.example.doanmb.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.databinding.FragmentAdminProfileBinding;
import com.example.doanmb.ui.base.BaseFragment;

import java.text.NumberFormat;
import java.util.Locale;

/** Trang cá nhân Admin — MVVM. */
public class AdminProfileFragment extends BaseFragment {

    private FragmentAdminProfileBinding binding;
    private AdminProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminProfileViewModel.class);

        viewModel.getName().observe(getViewLifecycleOwner(), name -> {
            binding.tvAdminProfileName.setText(name);
            binding.tvInfoName.setText(name);
        });
        viewModel.getEmail().observe(getViewLifecycleOwner(), email -> {
            binding.tvAdminProfileEmail.setText(email);
            binding.tvInfoEmail.setText(email);
        });
        viewModel.getAppWallet().observe(getViewLifecycleOwner(), bal -> {
            String formatted = NumberFormat.getInstance(new Locale("vi", "VN")).format(bal != null ? bal : 0L);
            binding.tvAppWalletBalance.setText(formatted + " đ");
        });

        viewModel.load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
