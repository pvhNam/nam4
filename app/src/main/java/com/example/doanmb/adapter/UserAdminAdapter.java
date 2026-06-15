package com.example.doanmb.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.ViewHolder> {

    public interface OnRoleChangeListener {
        void onRoleChange(String userId, String newRole);
    }

    public interface OnDeleteListener {
        void onDelete(String userId, String userName);
    }

    private List<Map<String, Object>> allUsers = new ArrayList<>();
    private List<String> allUserIds = new ArrayList<>();
    private List<Map<String, Object>> filteredUsers = new ArrayList<>();
    private List<String> filteredUserIds = new ArrayList<>();

    private OnRoleChangeListener roleListener;
    private OnDeleteListener deleteListener;

    private String currentFilter = "ALL";
    private String currentSearch = "";

    public UserAdminAdapter(List<Map<String, Object>> users, List<String> userIds,
                            OnRoleChangeListener roleListener, OnDeleteListener deleteListener) {
        this.roleListener = roleListener;
        this.deleteListener = deleteListener;
        updateList(users, userIds);
    }

    public void updateList(List<Map<String, Object>> newUsers, List<String> newIds) {
        this.allUsers = new ArrayList<>(newUsers);
        this.allUserIds = new ArrayList<>(newIds);
        applyFilter();
    }

    public void setFilter(String filter) {
        this.currentFilter = filter;
        applyFilter();
    }

    public void setSearch(String query) {
        this.currentSearch = query.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        filteredUsers.clear();
        filteredUserIds.clear();

        for (int i = 0; i < allUsers.size(); i++) {
            Map<String, Object> user = allUsers.get(i);
            String role = getStr(user, "role", "CUSTOMER").toUpperCase();

            boolean matchRole = currentFilter.equals("ALL") || role.equals(currentFilter);

            boolean matchSearch = true;
            if (!currentSearch.isEmpty()) {
                String name = getStr(user, "name", "").toLowerCase();
                String email = getStr(user, "email", "").toLowerCase();
                String phone = getStr(user, "phone", "").toLowerCase();
                matchSearch = name.contains(currentSearch)
                        || email.contains(currentSearch)
                        || phone.contains(currentSearch);
            }

            if (matchRole && matchSearch) {
                filteredUsers.add(user);
                filteredUserIds.add(allUserIds.get(i));
            }
        }

        notifyDataSetChanged();
    }

    public int getFilteredCount() {
        return filteredUsers.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = filteredUsers.get(position);
        String userId = filteredUserIds.get(position);
        Context ctx = holder.itemView.getContext();

        String name = getStr(user, "name", "Không có tên");
        String email = getStr(user, "email", "");
        String phone = getStr(user, "phone", "");
        String role = getStr(user, "role", "CUSTOMER").toUpperCase();
        String avatarUrl = getStr(user, "avatarUrl", "");

        holder.tvName.setText(name);
        holder.tvEmail.setText(email);
        holder.tvPhone.setText(phone.isEmpty() ? "Chưa có SĐT" : phone);
        holder.tvRole.setText(role);

        applyRoleStyle(ctx, holder.tvRole, role);

        if (!avatarUrl.isEmpty()) {
            Glide.with(ctx).load(avatarUrl).circleCrop()
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }

        holder.itemView.setOnClickListener(v -> showRoleDialog(ctx, userId, role));

        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(userId, name);
        });
    }

    private void showRoleDialog(Context ctx, String userId, String currentRole) {
        String[] roles = {"ADMIN", "CUSTOMER"};
        int currentIndex = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(currentRole)) { currentIndex = i; break; }
        }
        final int[] selected = {currentIndex};

        new AlertDialog.Builder(ctx)
                .setTitle("Đổi quyền người dùng")
                .setSingleChoiceItems(roles, currentIndex, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    if (roleListener != null) roleListener.onRoleChange(userId, roles[selected[0]]);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyRoleStyle(Context ctx, TextView tv, String role) {
        if ("ADMIN".equals(role)) {
            tv.setBackgroundColor(ctx.getColor(R.color.role_admin_bg));
            tv.setTextColor(ctx.getColor(R.color.role_admin_text));
        } else {
            tv.setBackgroundColor(ctx.getColor(R.color.role_customer_bg));
            tv.setTextColor(ctx.getColor(R.color.role_customer_text));
        }
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return (v != null) ? v.toString() : def;
    }

    @Override
    public int getItemCount() { return filteredUsers.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivDelete;
        TextView tvName, tvEmail, tvPhone, tvRole;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            ivDelete = itemView.findViewById(R.id.iv_delete_user);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvPhone = itemView.findViewById(R.id.tv_user_phone);
            tvRole = itemView.findViewById(R.id.tv_user_role);
        }
    }
}
