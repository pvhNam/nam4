package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.MainActivity;
import com.example.doanmb.R;
import com.example.doanmb.ui.activity.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/** Tab tài khoản của tài xế: hiển thị hồ sơ + đăng xuất. */
public class DriverProfileFragment extends Fragment {

    private TextView tvName, tvCarType, tvCccd, tvLicense, tvPhone;
    private ImageView ivAvatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_profile, container, false);

        tvName = view.findViewById(R.id.tv_dp_name);
        tvCarType = view.findViewById(R.id.tv_dp_cartype);
        tvCccd = view.findViewById(R.id.tv_dp_cccd);
        tvLicense = view.findViewById(R.id.tv_dp_license);
        tvPhone = view.findViewById(R.id.tv_dp_phone);
        ivAvatar = view.findViewById(R.id.iv_dp_avatar);

        view.findViewById(R.id.btn_dp_logout).setOnClickListener(v -> logout());
        view.findViewById(R.id.btn_dp_switch_user).setOnClickListener(v -> switchToUserMode());

        loadInfo();
        return view;
    }

    private void loadInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || !doc.exists()) return;
                    tvName.setText(text(doc.getString("name"), "Tài xế"));
                    tvCarType.setText(text(doc.getString("driverCarType"), "--"));
                    tvCccd.setText(text(doc.getString("cccd"), "--"));
                    tvLicense.setText(text(doc.getString("licenseNumber"), "--"));
                    tvPhone.setText(text(doc.getString("phone"), "--"));
                    String avatar = doc.getString("avatarUrl");
                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(this).load(avatar).into(ivAvatar);
                    }
                });
    }

    private String text(String value, String def) {
        return value != null && !value.isEmpty() ? value : def;
    }

    /** Quay về giao diện khách hàng, vẫn giữ đăng nhập. */
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
}
