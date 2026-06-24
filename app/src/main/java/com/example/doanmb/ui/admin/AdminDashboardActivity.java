package com.example.doanmb.ui.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.doanmb.R;
import com.example.doanmb.databinding.ActivityAdminDashboardBinding;
import com.example.doanmb.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Khung giao diện Admin (MVVM): bottom nav + vùng fragment. Tên admin lấy qua
 * {@link AdminDashboardViewModel}.
 */
public class AdminDashboardActivity extends AppCompatActivity
        implements AdminOverviewFragment.OnQuickNavListener {

    private ActivityAdminDashboardBinding binding;
    private AdminDashboardViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewModel = new ViewModelProvider(this).get(AdminDashboardViewModel.class);
        viewModel.getAdminName().observe(this, name -> binding.tvAdminName.setText(name));
        viewModel.loadAdminName();

        binding.btnAdminLogout.setOnClickListener(v -> logout());
        binding.adminBottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });

        switchFragment(R.id.nav_admin_overview);
        binding.adminBottomNav.setSelectedItemId(R.id.nav_admin_overview);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                if (binding.adminBottomNav.getSelectedItemId() != R.id.nav_admin_overview) {
                    binding.adminBottomNav.setSelectedItemId(R.id.nav_admin_overview);
                } else {
                    new MaterialAlertDialogBuilder(AdminDashboardActivity.this)
                            .setTitle("Thoát Admin")
                            .setMessage("Bạn có muốn thoát khỏi trang quản trị không?")
                            .setPositiveButton("Thoát", (d, w) -> finish())
                            .setNegativeButton("Ở lại", null)
                            .show();
                }
            }
        });
    }

    private void switchFragment(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_admin_users) {
            fragment = new AdminUsersFragment();
        } else if (itemId == R.id.nav_admin_cars) {
            fragment = new AdminCarsFragment();
        } else if (itemId == R.id.nav_admin_orders) {
            fragment = new AdminOrdersFragment();
        } else if (itemId == R.id.nav_admin_reports) {
            fragment = new AdminReportsFragment();
        } else if (itemId == R.id.nav_admin_profile) {
            fragment = new AdminProfileFragment();
        } else if (itemId == R.id.nav_admin_driver_approval) {
            fragment = new AdminDriverApprovalFragment();
        } else {
            fragment = new AdminOverviewFragment();
        }

        boolean addToBackStack = (itemId == R.id.nav_admin_driver_approval);
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                .replace(R.id.admin_fragment_container, fragment);
        if (addToBackStack) tx.addToBackStack(null);
        tx.commit();
    }

    @Override
    public void navigateTo(int itemId) {
        switchFragment(itemId);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
