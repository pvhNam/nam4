package com.example.doanmb.ui.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminUserDetailFragment extends Fragment {

    private static final String ARG_USER_ID = "userId";

    private String userId;
    private FirebaseFirestore db;

    private ImageView ivAvatar;
    private TextView tvName, tvRole, tvEmail, tvPhone, tvAddress;
    private Button btnChangeRole;

    private String currentRole = "CUSTOMER";

    public static AdminUserDetailFragment newInstance(String userId) {
        AdminUserDetailFragment f = new AdminUserDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_user_detail, container, false);

        ivAvatar = view.findViewById(R.id.iv_detail_avatar);
        tvName = view.findViewById(R.id.tv_detail_name);
        tvRole = view.findViewById(R.id.tv_detail_role);
        tvEmail = view.findViewById(R.id.tv_detail_email);
        tvPhone = view.findViewById(R.id.tv_detail_phone);
        tvAddress = view.findViewById(R.id.tv_detail_address);
        btnChangeRole = view.findViewById(R.id.btn_change_role);

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnChangeRole.setOnClickListener(v -> showRoleDialog());

        loadUser();
        return view;
    }

    private void loadUser() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || doc == null) return;

                    String name = getStr(doc.getString("name"), "Không có tên");
                    String email = getStr(doc.getString("email"), "—");
                    String phone = getStr(doc.getString("phone"), "—");
                    String address = getStr(doc.getString("address"), "—");
                    String avatarUrl = getStr(doc.getString("avatarUrl"), "");
                    currentRole = getStr(doc.getString("role"), "CUSTOMER").toUpperCase();

                    tvName.setText(name);
                    tvEmail.setText(email);
                    tvPhone.setText(phone);
                    tvAddress.setText(address);
                    tvRole.setText(currentRole);
                    applyRoleStyle(currentRole);

                    if (!avatarUrl.isEmpty()) {
                        Glide.with(this).load(avatarUrl).circleCrop()
                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                .into(ivAvatar);
                    }
                });
    }

    private void showRoleDialog() {
        String[] roles = {"ADMIN", "CUSTOMER"};
        int currentIndex = currentRole.equals("ADMIN") ? 0 : 1;
        final int[] selected = {currentIndex};

        new AlertDialog.Builder(requireContext())
                .setTitle("Đổi quyền người dùng")
                .setSingleChoiceItems(roles, currentIndex, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newRole = roles[selected[0]];
                    db.collection("users").document(userId)
                            .update("role", newRole)
                            .addOnSuccessListener(v -> {
                                if (!isAdded()) return;
                                currentRole = newRole;
                                tvRole.setText(currentRole);
                                applyRoleStyle(currentRole);
                                Toast.makeText(getContext(), "Đã đổi quyền thành " + newRole, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyRoleStyle(String role) {
        if ("ADMIN".equals(role)) {
            tvRole.setBackgroundColor(requireContext().getColor(R.color.role_admin_bg));
            tvRole.setTextColor(requireContext().getColor(R.color.role_admin_text));
        } else {
            tvRole.setBackgroundColor(requireContext().getColor(R.color.role_customer_bg));
            tvRole.setTextColor(requireContext().getColor(R.color.role_customer_text));
        }
    }

    private String getStr(String value, String def) {
        return (value != null && !value.isEmpty()) ? value : def;
    }
}
