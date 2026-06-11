package com.example.doanmb.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.doanmb.R;
import com.example.doanmb.ui.fragment.DriverHomeFragment;
import com.example.doanmb.ui.fragment.DriverPostFragment;
import com.example.doanmb.ui.fragment.DriverProfileFragment;
import com.example.doanmb.ui.fragment.DriverTripsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class DriverDashboardActivity extends AppCompatActivity {

    private TextView tvDriverName, tvOnlineLabel;
    private SwitchMaterial switchOnline;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { logout(); return; }
        uid = user.getUid();

        tvDriverName = findViewById(R.id.tv_driver_name);
        tvOnlineLabel = findViewById(R.id.tv_online_label);
        switchOnline = findViewById(R.id.switch_online);
        BottomNavigationView bottomNav = findViewById(R.id.driver_bottom_nav);
        Button btnLogout = findViewById(R.id.btn_driver_logout);

        loadDriverInfo();

        btnLogout.setOnClickListener(v -> logout());

        switchOnline.setOnClickListener(v -> {
            boolean online = switchOnline.isChecked();
            tvOnlineLabel.setText(online ? "Online" : "Offline");
            db.collection("users").document(uid).update("driverOnline", online);
        });

        bottomNav.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });

        switchFragment(R.id.nav_driver_home);
        bottomNav.setSelectedItemId(R.id.nav_driver_home);
    }

    private void loadDriverInfo() {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.getString("name") != null) tvDriverName.setText(doc.getString("name"));
            Boolean online = doc.getBoolean("driverOnline");
            boolean isOnline = online != null && online;
            switchOnline.setChecked(isOnline);
            tvOnlineLabel.setText(isOnline ? "Online" : "Offline");
        });
    }

    private void switchFragment(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_driver_trips) {
            fragment = new DriverTripsFragment();
        } else if (itemId == R.id.nav_driver_post) {
            fragment = new DriverPostFragment();
        } else if (itemId == R.id.nav_driver_account) {
            fragment = new DriverProfileFragment();
        } else {
            fragment = new DriverHomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.driver_fragment_container, fragment)
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
