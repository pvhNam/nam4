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

import java.util.List;
import java.util.Map;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.ViewHolder> {

    public interface OnRoleChangeListener {
        void onRoleChange(String userId, String newRole);
    }

    private List<Map<String, Object>> users;
    private List<String> userIds;
    private OnRoleChangeListener listener;

    public UserAdminAdapter(List<Map<String, Object>> users, List<String> userIds, OnRoleChangeListener listener) {
        this.users = users;
        this.userIds = userIds;
        this.listener = listener;
    }

    public void updateList(List<Map<String, Object>> newUsers, List<String> newIds) {
        this.users = newUsers;
        this.userIds = newIds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_admin, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);
        String userId = userIds.get(position);
        Context ctx = holder.itemView.getContext();

        String name = getStr(user, "name", "Không có tên");
        String email = getStr(user, "email", "");
        String phone = getStr(user, "phone", "");
        String role = normalizeRole(getStr(user, "role", "CUSTOMER"));
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
    }

    private void showRoleDialog(Context ctx, String userId, String currentRole) {
        currentRole = normalizeRole(currentRole);
        String[] roles = {"ADMIN", "DRIVER", "CUSTOMER"};
        int currentIndex = 0;
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(currentRole)) { currentIndex = i; break; }
        }
        final int[] selected = {currentIndex};

        new AlertDialog.Builder(ctx)
                .setTitle("Đổi quyền người dùng")
                .setSingleChoiceItems(roles, currentIndex, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    if (listener != null) listener.onRoleChange(userId, roles[selected[0]]);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyRoleStyle(Context ctx, TextView tv, String role) {
        switch (role) {
            case "ADMIN":
                tv.setBackgroundColor(ctx.getColor(R.color.role_admin_bg));
                tv.setTextColor(ctx.getColor(R.color.role_admin_text));
                break;
            case "DRIVER":
                tv.setBackgroundColor(ctx.getColor(R.color.role_staff_bg));
                tv.setTextColor(ctx.getColor(R.color.role_staff_text));
                break;
            default:
                tv.setBackgroundColor(ctx.getColor(R.color.role_customer_bg));
                tv.setTextColor(ctx.getColor(R.color.role_customer_text));
                break;
        }
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object v = map.get(key);
        return (v != null) ? v.toString() : def;
    }

    private String normalizeRole(String role) {
        if ("STAFF".equals(role)) return "DRIVER";
        return role;
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvName, tvEmail, tvPhone, tvRole;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvEmail = itemView.findViewById(R.id.tv_user_email);
            tvPhone = itemView.findViewById(R.id.tv_user_phone);
            tvRole = itemView.findViewById(R.id.tv_user_role);
        }
    }
}
