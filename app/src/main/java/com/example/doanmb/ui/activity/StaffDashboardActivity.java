package com.example.doanmb.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.doanmb.R;
import com.example.doanmb.ui.fragment.StaffApproveFragment;
import com.example.doanmb.ui.fragment.StaffOrdersFragment;
import com.example.doanmb.ui.fragment.StaffProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffDashboardActivity extends AppCompatActivity {

    private TextView tvStaffName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_dashboard);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tvStaffName = findViewById(R.id.tv_staff_name);
        BottomNavigationView bottomNav = findViewById(R.id.staff_bottom_nav);
        Button btnLogout = findViewById(R.id.btn_staff_logout);

        loadStaffName();

        btnLogout.setOnClickListener(v -> logout());

        bottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });

        switchFragment(R.id.nav_staff_approve);
        bottomNav.setSelectedItemId(R.id.nav_staff_approve);
    }

    private void loadStaffName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.getString("name") != null) {
                        tvStaffName.setText(doc.getString("name"));
                    }
                });
    }

    private void switchFragment(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_staff_orders) {
            fragment = new StaffOrdersFragment();
        } else if (itemId == R.id.nav_staff_account) {
            fragment = new StaffProfileFragment();
        } else {
            fragment = new StaffApproveFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.staff_fragment_container, fragment)
                .commit();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
