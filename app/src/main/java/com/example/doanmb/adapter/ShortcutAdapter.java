package com.example.doanmb.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.doanmb.R;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.activity.ChatDetailActivity;

import java.util.List;
import java.util.Map;

public class ShortcutAdapter extends RecyclerView.Adapter<ShortcutAdapter.VH> {
    private List<Map<String, Object>> list;

    public ShortcutAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shortcut_bubble, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> item = list.get(position);
        String name = (String) item.get("partnerName");
        String avatar = (String) item.get("partnerAvatar");

        h.tvName.setText(name != null ? name : "U");
        String initial = name != null && !name.isEmpty() ? String.valueOf(name.charAt(0)).toUpperCase() : "U";
        h.tvAvatar.setText(initial);
        
        if (avatar != null && !avatar.isEmpty()) {
            h.tvAvatar.setVisibility(View.GONE);
            h.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(h.ivAvatar.getContext()).load(avatar).transform(new CircleCrop()).into(h.ivAvatar);
        } else {
            h.tvAvatar.setVisibility(View.VISIBLE);
            h.ivAvatar.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatDetailActivity.class);
            intent.putExtra("ROOM_ID", (String) item.get("roomId"));
            intent.putExtra("PARTNER_ID", (String) item.get("partnerId"));
            intent.putExtra("PARTNER_NAME", name);
            
            Car car = new Car((String)item.get("carName"), "", "", 0);
            car.setId((String)item.get("carId"));
            car.setImageUrl((String)item.get("carImage"));
            intent.putExtra("CAR_DATA", car);
            
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName;
        ImageView ivAvatar;

        VH(@NonNull View v) {
            super(v);
            tvAvatar = v.findViewById(R.id.tvShortcutAvatar);
            ivAvatar = v.findViewById(R.id.ivShortcutAvatar);
            tvName = v.findViewById(R.id.tvShortcutName);
        }
    }
}
