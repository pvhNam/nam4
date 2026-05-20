package com.example.doanmb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.model.Car;

import java.util.List;

public class CarSaleAdapter extends RecyclerView.Adapter<CarSaleAdapter.CarSaleViewHolder> {

    private List<Car> carList;
    private OnItemClickListener listener; // Thêm Listener

    // Tạo Interface để xử lý Click
    public interface OnItemClickListener {
        void onItemClick(Car car);
    }

    // Cập nhật Constructor
    public CarSaleAdapter(List<Car> carList, OnItemClickListener listener) {
        this.carList = carList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarSaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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

        // Bắt sự kiện click vào nút "XEM CHI TIẾT" hoặc toàn bộ Item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(car);
            }
        });

        // Bắt sự kiện click vào nút bấm cụ thể
        holder.btnViewDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(car);
            }
        });
    }

    @Override
    public int getItemCount() {
        return carList.size();
    }

    public static class CarSaleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvInfo;
        Button btnViewDetails; // Khai báo thêm nút bấm

        public CarSaleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivCarSale);
            tvName = itemView.findViewById(R.id.tvCarSaleName);
            tvPrice = itemView.findViewById(R.id.tvCarSalePrice);
            tvInfo = itemView.findViewById(R.id.tvCarSaleInfo);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails); // Ánh xạ nút
        }
    }
    // Thêm hàm này để cập nhật lại danh sách khi tìm kiếm
    public void filterList(List<Car> filteredList) {
        this.carList = filteredList;
        notifyDataSetChanged();
    }
}