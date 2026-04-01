package com.example.doanmb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CarRentalAdapter extends RecyclerView.Adapter<CarRentalAdapter.CarViewHolder> {

    private List<Car> carList;

    public CarRentalAdapter(List<Car> carList) {
        this.carList = carList;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_car_rental, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
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

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvInfo;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            // Các ID này phải khớp với id trong file item_car_rental.xml
            ivImage = itemView.findViewById(R.id.ivCarRental);
            tvName = itemView.findViewById(R.id.tvCarRentalName);
            tvPrice = itemView.findViewById(R.id.tvCarRentalPrice);
            tvInfo = itemView.findViewById(R.id.tvCarRentalInfo);
        }
    }
}