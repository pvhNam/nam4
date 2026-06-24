package com.example.doanmb.ui.driver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.doanmb.R;
import com.example.doanmb.databinding.ActivityDriverDashboardBinding;
import com.example.doanmb.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Khung giao diện tài xế: vùng fragment + thanh điều hướng 5 tab. Mỗi tab (Trang
 * chủ / Chuyến / Bản đồ / Thu nhập / Cá nhân) tự vẽ header xanh theo thiết kế
 * driver1-5. Đây là khung điều hướng nên không cần ViewModel riêng.
 */
public class DriverDashboardActivity extends AppCompatActivity {

    private ActivityDriverDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        getWindow().setStatusBarColor(0xFF2E6BF0);
        getWindow().setNavigationBarColor(0xFFFFFFFF);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        binding.driverBottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });
        binding.driverBottomNav.setSelectedItemId(R.id.nav_driver_home);
    }

    private void switchFragment(int itemId) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment;
        if (itemId == R.id.nav_driver_trips) {
            fragment = new DriverTripsFragment();
        } else if (itemId == R.id.nav_driver_map) {
            fragment = new DriverMapFragment();
        } else if (itemId == R.id.nav_driver_earnings) {
            fragment = new DriverEarningsFragment();
        } else if (itemId == R.id.nav_driver_account) {
            fragment = new DriverProfileFragment();
        } else {
            fragment = new DriverHomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.driver_fragment_container, fragment)
                .commit();
    }

    /** Chuyển sang tab Cá nhân (bấm avatar ở trang chủ). */
    public void openProfileTab() {
        binding.driverBottomNav.setSelectedItemId(R.id.nav_driver_account);
    }

    /** Mở form "Đăng kí cho thuê xe" từ trang Cá nhân (driver5). */
    public void openPostForm() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.driver_fragment_container, new DriverPostFragment())
                .addToBackStack("post")
                .commit();
    }
}
