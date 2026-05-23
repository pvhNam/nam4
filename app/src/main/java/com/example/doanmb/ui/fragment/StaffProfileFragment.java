package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.doanmb.R;
import com.example.doanmb.ui.activity.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffProfileFragment extends Fragment {

    private TextView tvName, tvEmail, tvPhone;
    private Button btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_staff_profile, container, false);

        tvName = view.findViewById(R.id.tv_staff_profile_name);
        tvEmail = view.findViewById(R.id.tv_staff_email);
        tvPhone = view.findViewById(R.id.tv_staff_phone);
        btnLogout = view.findViewById(R.id.btn_staff_logout_profile);

        loadProfile();

        btnLogout.setOnClickListener(v -> logout());

        return view;
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;
                    tvName.setText(doc.getString("name") != null ? doc.getString("name") : "Nhân viên");
                    tvEmail.setText(doc.getString("email") != null ? doc.getString("email") : "--");
                    String phone = doc.getString("phone");
                    tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Chưa có SĐT");
                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
