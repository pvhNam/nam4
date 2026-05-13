package com.example.doanmb;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CarRentalAdapter extends RecyclerView.Adapter<CarRentalAdapter.CarViewHolder> {

    private List<Car> carList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Car car);
    }

    public CarRentalAdapter(List<Car> carList) {
        this.carList = carList;
        this.listener = null;
    }

    public CarRentalAdapter(List<Car> carList, OnItemClickListener listener) {
        this.carList  = carList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car_rental, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = carList.get(position);
        holder.tvName.setText(car.getName());
        holder.tvPrice.setText(car.getPrice());
        holder.tvInfo.setText(car.getInfo());
        holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(car);
        });

        if (holder.btnRentNow != null) {
            holder.btnRentNow.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(car);
            });
        }
    }

    @Override
    public int getItemCount() { return carList.size(); }

    public void filterList(List<Car> filteredList) {
        this.carList = filteredList;
        notifyDataSetChanged();
    }

    public static class CarViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvInfo;
        Button btnRentNow;

        public CarViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage    = itemView.findViewById(R.id.ivCarRental);
            tvName     = itemView.findViewById(R.id.tvCarRentalName);
            tvPrice    = itemView.findViewById(R.id.tvCarRentalPrice);
            tvInfo     = itemView.findViewById(R.id.tvCarRentalInfo);
            btnRentNow = itemView.findViewById(R.id.btnRentNow);
        }
    }
}