package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.ui.activity.LoginActivity;
import com.example.doanmb.ui.activity.RegisterActivity;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn;
    private FrameLayout layoutLoggedIn;

    // 3 Sub-screens quản lý layer giao diện
    private ScrollView layoutMainProfile;
    private ConstraintLayout layoutAccountSettings;
    private RelativeLayout layoutPersonalInfo;

    // Views màn 1
    private CardView cardUserProfile;
    private TextView tvProfileNameMain, tvPhoneVerifiedBadge;
    private ImageView ivAvatarMain;
    private Button btnLogin, btnRegister, btnLogout;

    // Views màn 2
    private ImageView ivAvatarSettings;
    private CardView ivChangeAvatarTrigger;
    private RelativeLayout menuPersonalInfoClick;
    private ImageView btnBackToMain; // Đã đổi sang ImageView để tránh lỗi theme

    // Views màn 3
    private CardView btnBackToSettings;
    private EditText edtInfoName, edtInfoDob, edtInfoGender, edtInfoPhone;
    private Button btnSavePersonalInfo;

    private FirebaseFirestore db;
    private String currentUserId = "";

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);

        layoutMainProfile = view.findViewById(R.id.layout_main_profile);
        layoutAccountSettings = view.findViewById(R.id.layout_account_settings);
        layoutPersonalInfo = view.findViewById(R.id.layout_personal_info);

        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogout = view.findViewById(R.id.btn_logout);
        cardUserProfile = view.findViewById(R.id.card_user_profile);
        tvProfileNameMain = view.findViewById(R.id.tv_profile_name_main);
        tvPhoneVerifiedBadge = view.findViewById(R.id.tv_phone_verified_badge);
        ivAvatarMain = view.findViewById(R.id.iv_avatar_main);

        ivAvatarSettings = view.findViewById(R.id.iv_avatar_settings);
        ivChangeAvatarTrigger = view.findViewById(R.id.iv_change_avatar_trigger);
        menuPersonalInfoClick = view.findViewById(R.id.menu_personal_info);
        btnBackToMain = view.findViewById(R.id.btn_back_to_main);

        btnBackToSettings = view.findViewById(R.id.btn_back_to_settings);
        edtInfoName = view.findViewById(R.id.edt_info_name);
        edtInfoDob = view.findViewById(R.id.edt_info_dob);
        edtInfoGender = view.findViewById(R.id.edt_info_gender);
        edtInfoPhone = view.findViewById(R.id.edt_info_phone);
        btnSavePersonalInfo = view.findViewById(R.id.btn_save_personal_info);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            switchSubScreen(1);
            checkLoginStatus();
        });

        cardUserProfile.setOnClickListener(v -> switchSubScreen(2));
        btnBackToMain.setOnClickListener(v -> switchSubScreen(1));
        menuPersonalInfoClick.setOnClickListener(v -> switchSubScreen(3));
        btnBackToSettings.setOnClickListener(v -> switchSubScreen(2));

        ivChangeAvatarTrigger.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSavePersonalInfo.setOnClickListener(v -> saveUserInformationToFirestore());
    }

    private void switchSubScreen(int screenId) {
        layoutMainProfile.setVisibility(screenId == 1 ? View.VISIBLE : View.GONE);
        layoutAccountSettings.setVisibility(screenId == 2 ? View.VISIBLE : View.GONE);
        layoutPersonalInfo.setVisibility(screenId == 3 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            layoutNotLoggedIn.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            currentUserId = user.getUid();
            loadUserInfo(user.getUid());
        } else {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void loadUserInfo(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(requireActivity(), doc -> {
                    if (!isAdded() || !doc.exists()) return;

                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String dob = doc.getString("dob");
                    String gender = doc.getString("gender");
                    String avatarUrl = doc.getString("avatarUrl");
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");

                    tvProfileNameMain.setText(name != null ? name : "Chưa đặt tên");
                    if (phoneVerified != null && phoneVerified) {
                        tvPhoneVerifiedBadge.setText("✓ Đã xác thực");
                        tvPhoneVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    } else {
                        tvPhoneVerifiedBadge.setText("✕ Chưa xác thực số điện thoại");
                        tvPhoneVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#E53935"));
                    }

                    edtInfoName.setText(name != null ? name : "");
                    edtInfoPhone.setText(phone != null ? phone : "");
                    edtInfoDob.setText(dob != null ? dob : "");
                    edtInfoGender.setText(gender != null ? gender : "Nam");

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (ivAvatarMain != null) {
                            Glide.with(ProfileFragment.this).load(avatarUrl).circleCrop().into(ivAvatarMain);
                        }
                        if (ivAvatarSettings != null) {
                            Glide.with(ProfileFragment.this).load(avatarUrl).circleCrop().into(ivAvatarSettings);
                        }
                    }
                });
    }

    private void uploadAvatar(Uri uri) {
        if (currentUserId.isEmpty()) return;

        Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        CloudinaryHelper.uploadImage(requireContext(), uri, new CloudinaryHelper.OnUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (!isAdded() || getActivity() == null) return;

                db.collection("users").document(currentUserId)
                        .update("avatarUrl", imageUrl)
                        .addOnSuccessListener(requireActivity(), unused -> {
                            if (!isAdded()) return;

                            Glide.with(ProfileFragment.this).load(imageUrl).circleCrop().into(ivAvatarMain);
                            Glide.with(ProfileFragment.this).load(imageUrl).circleCrop().into(ivAvatarSettings);
                            Toast.makeText(getContext(), "✅ Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserInformationToFirestore() {
        if (currentUserId.isEmpty()) return;

        String updatedName = edtInfoName.getText().toString().trim();
        String updatedDob = edtInfoDob.getText().toString().trim();
        String updatedGender = edtInfoGender.getText().toString().trim();
        String updatedPhone = edtInfoPhone.getText().toString().trim();

        if (updatedName.isEmpty()) {
            Toast.makeText(getContext(), "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", updatedName);
        updateData.put("dob", updatedDob);
        updateData.put("gender", updatedGender);
        updateData.put("phone", updatedPhone);

        Toast.makeText(getContext(), "Đang lưu thay đổi...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(currentUserId)
                .update(updateData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), "✅ Lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                    loadUserInfo(currentUserId);
                    switchSubScreen(2);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}