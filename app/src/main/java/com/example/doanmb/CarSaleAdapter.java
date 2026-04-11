package com.example.doanmb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CarSaleAdapter extends RecyclerView.Adapter<CarSaleAdapter.CarSaleViewHolder> {

    private List<Car> carList;

    public CarSaleAdapter(List<Car> carList) {
        this.carList = carList;
    }

    @NonNull
    @Override
    public CarSaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Trỏ đến layout item_car_sale.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car_sale, parent, false);
        return new CarSaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarSaleViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.tvName.setText(car.getName());
        holder.tvPrice.setText(car.getPrice());
        holder.tvInfo.setText(car.getInfo());
        holder.ivImage.setImageResource(car.getImageResId());
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public static class CarSaleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvInfo;

        public CarSaleViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ các ID từ file item_car_sale.xml
            ivImage = itemView.findViewById(R.id.ivCarSale);
            tvName = itemView.findViewById(R.id.tvCarSaleName);
            tvPrice = itemView.findViewById(R.id.tvCarSalePrice);
            tvInfo = itemView.findViewById(R.id.tvCarSaleInfo);
        }
    }
}