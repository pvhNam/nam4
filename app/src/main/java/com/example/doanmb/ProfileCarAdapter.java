package com.example.doanmb;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProfileCarAdapter extends RecyclerView.Adapter<ProfileCarAdapter.ViewHolder> {

    private List<Car> carList;

    public ProfileCarAdapter(List<Car> carList) {
        this.carList = carList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.tvName.setText(car.getName());
        holder.tvPrice.setText(car.getPrice());
        holder.tvInfo.setText(car.getInfo() != null ? car.getInfo() : "");
        String type = car.getType() != null ? car.getType() : "";
        if (type.equals("sale") || type.contains("bán") || type.contains("ban")) {
            holder.tvType.setText("Cần bán");
            holder.tvType.setBackgroundColor(Color.parseColor("#1976D2"));
        } else {
            holder.tvType.setText("Cho thuê");
            holder.tvType.setBackgroundColor(Color.parseColor("#4CAF50"));
        }
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public void updateList(List<Car> newList) {
        this.carList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvInfo, tvType;
        ImageView ivCar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCar = itemView.findViewById(R.id.iv_car_profile);
            tvName = itemView.findViewById(R.id.tv_car_profile_name);
            tvPrice = itemView.findViewById(R.id.tv_car_profile_price);
            tvInfo = itemView.findViewById(R.id.tv_car_profile_info);
            tvType = itemView.findViewById(R.id.tv_car_profile_type);
        }
    }
}
