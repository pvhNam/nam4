package com.example.doanmb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.model.User;

import java.util.List;

/** Danh sách hồ sơ tài xế chờ duyệt cho Admin/Staff. */
public class DriverApprovalAdapter extends RecyclerView.Adapter<DriverApprovalAdapter.VH> {

    public interface OnDecisionListener {
        void onApprove(User u);
        void onReject(User u);
    }

    private final List<User> pending;
    private final OnDecisionListener listener;

    public DriverApprovalAdapter(List<User> pending, OnDecisionListener listener) {
        this.pending = pending;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver_pending, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        User u = pending.get(position);
        h.tvName.setText(u.getName() != null ? u.getName() : "(không tên)");
        h.tvInfo.setText("CCCD: " + safe(u.getCccd())
                + "\nGPLX: " + safe(u.getLicenseNumber())
                + "\nLoại xe: " + safe(u.getDriverCarType()));

        loadImg(h.ivCccd, u.getCccdImageUrl());
        loadImg(h.ivLicense, u.getLicenseImageUrl());

        h.btnApprove.setOnClickListener(v -> { if (listener != null) listener.onApprove(u); });
        h.btnReject.setOnClickListener(v -> { if (listener != null) listener.onReject(u); });
    }

    private void loadImg(ImageView iv, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(iv.getContext()).load(url).into(iv);
        } else {
            iv.setImageResource(android.R.drawable.ic_menu_report_image);
        }
    }

    private String safe(String s) { return s != null && !s.isEmpty() ? s : "--"; }

    @Override
    public int getItemCount() { return pending.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo;
        ImageView ivCccd, ivLicense;
        Button btnApprove, btnReject;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_dp_name);
            tvInfo = v.findViewById(R.id.tv_dp_info);
            ivCccd = v.findViewById(R.id.iv_dp_cccd);
            ivLicense = v.findViewById(R.id.iv_dp_license);
            btnApprove = v.findViewById(R.id.btn_dp_approve);
            btnReject = v.findViewById(R.id.btn_dp_reject);
        }
    }
}
