package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.UserAdminAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private TextView tvUserCount, tvEmpty;
    private EditText etSearch;
    private Button btnTabAll, btnTabAdmin, btnTabCustomer;
    private UserAdminAdapter adapter;
    private List<Map<String, Object>> userList = new ArrayList<>();
    private List<String> userIds = new ArrayList<>();
    private FirebaseFirestore db;
    private String activeFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_users, container, false);
        db = FirebaseFirestore.getInstance();

        rvUsers = view.findViewById(R.id.rv_users);
        tvUserCount = view.findViewById(R.id.tv_user_count);
        tvEmpty = view.findViewById(R.id.tv_empty_users);
        etSearch = view.findViewById(R.id.et_search);
        btnTabAll = view.findViewById(R.id.btn_tab_all);
        btnTabAdmin = view.findViewById(R.id.btn_tab_admin);
        btnTabCustomer = view.findViewById(R.id.btn_tab_customer);

        adapter = new UserAdminAdapter(userList, userIds, this::changeUserRole, this::confirmDeleteUser);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        rvUsers.setAdapter(adapter);

        setupTabs();
        setupSearch();
        loadUsers();
        return view;
    }

    private void setupTabs() {
        btnTabAll.setOnClickListener(v -> setActiveFilter("ALL"));
        btnTabAdmin.setOnClickListener(v -> setActiveFilter("ADMIN"));
        btnTabCustomer.setOnClickListener(v -> setActiveFilter("CUSTOMER"));
    }

    private void setActiveFilter(String filter) {
        activeFilter = filter;
        adapter.setFilter(filter);
        updateTabStyles();
        updateCountText();
    }

    private void updateTabStyles() {
        if (!isAdded()) return;
        int primaryColor = getResources().getColor(R.color.admin_primary, null);
        int adminBg = getResources().getColor(R.color.role_admin_bg, null);
        int customerBg = getResources().getColor(R.color.role_customer_bg, null);
        int customerText = getResources().getColor(R.color.role_customer_text, null);
        int white = getResources().getColor(android.R.color.white, null);

        // Reset all to inactive
        btnTabAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adminBg));
        btnTabAll.setTextColor(primaryColor);
        btnTabAdmin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(adminBg));
        btnTabAdmin.setTextColor(primaryColor);
        btnTabCustomer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(customerBg));
        btnTabCustomer.setTextColor(customerText);

        // Highlight active
        switch (activeFilter) {
            case "ALL":
                btnTabAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTabAll.setTextColor(white);
                break;
            case "ADMIN":
                btnTabAdmin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTabAdmin.setTextColor(white);
                break;
            case "CUSTOMER":
                btnTabCustomer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTabCustomer.setTextColor(white);
                break;
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.setSearch(s.toString());
                updateCountText();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        db.collection("users").get().addOnSuccessListener(snapshots -> {
            if (!isAdded()) return;
            userList.clear();
            userIds.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                userList.add(doc.getData());
                userIds.add(doc.getId());
            }
            adapter.updateList(userList, userIds);
            updateCountText();
        });
    }

    private void updateCountText() {
        if (!isAdded()) return;
        int count = adapter.getFilteredCount();
        tvUserCount.setText(count + " người dùng");
        tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        rvUsers.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private void changeUserRole(String userId, String newRole) {
        db.collection("users").document(userId)
                .update("role", newRole)
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Đã đổi quyền thành " + newRole, Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteUser(String userId, String userName) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc muốn xóa \"" + userName + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteUser(userId))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteUser(String userId) {
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(v -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                    loadUsers();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }
}
