package com.example.doanmb.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.model.Trip;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Hiển thị danh sách chuyến cho tài xế.
 * Nút hành động đổi theo trạng thái: Nhận chuyến / Hoàn thành chuyến / (ẩn).
 */
public class TripAdapter extends RecyclerView.Adapter<TripAdapter.VH> {

    public interface OnTripActionListener {
        /** Tài xế bấm nút hành động trên một chuyến. */
        void onAction(Trip trip);
    }

    private final List<Trip> trips;
    private final OnTripActionListener listener;
    private static final DecimalFormat MONEY = new DecimalFormat("#,###");

    public TripAdapter(List<Trip> trips, OnTripActionListener listener) {
        this.trips = trips;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Trip t = trips.get(position);

        h.tvCarType.setText(t.getCarType() != null ? t.getCarType() : "--");
        h.tvMode.setText(t.rentModeLabel());
        h.tvCustomer.setText("Khách: " + (t.getCustomerName() != null ? t.getCustomerName() : "--")
                + (t.getCustomerPhone() != null ? " · " + t.getCustomerPhone() : ""));
        h.tvPrice.setText(MONEY.format(t.getPrice()) + " đ");

        // Lộ trình / thời lượng
        if (Trip.MODE_DISTANCE.equals(t.getRentMode())) {
            String route = "Đón: " + safe(t.getPickup()) + "\nĐến: " + safe(t.getDestination());
            if (t.getDistanceKm() > 0) route += "  (" + t.getDistanceKm() + " km)";
            h.tvRoute.setText(route);
        } else {
            String unit = Trip.MODE_MONTH.equals(t.getRentMode()) ? "tháng" : "ngày";
            h.tvRoute.setText("Đón: " + safe(t.getPickup()) + "\nThời lượng: " + t.getDuration() + " " + unit);
        }

        // Trạng thái + nút hành động
        String status = t.getStatus() != null ? t.getStatus() : Trip.STATUS_WAITING;
        switch (status) {
            case Trip.STATUS_WAITING:
                styleStatus(h.tvStatus, "Chờ nhận", 0xFFFFF3E0, 0xFFE65100);
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Nhận chuyến");
                break;
            case Trip.STATUS_RUNNING:
                styleStatus(h.tvStatus, "Đang chạy", 0xFFE3F2FD, 0xFF1565C0);
                h.btnAction.setVisibility(View.VISIBLE);
                h.btnAction.setText("Hoàn thành chuyến");
                break;
            case Trip.STATUS_COMPLETED:
                styleStatus(h.tvStatus, "Hoàn thành", 0xFFE8F5E9, 0xFF2E7D32);
                h.btnAction.setVisibility(View.GONE);
                break;
            default:
                styleStatus(h.tvStatus, "Đã huỷ", 0xFFFFCDD2, 0xFFC62828);
                h.btnAction.setVisibility(View.GONE);
        }

        h.btnAction.setOnClickListener(v -> {
            if (listener != null) listener.onAction(t);
        });
    }

    private void styleStatus(TextView tv, String text, int bg, int fg) {
        tv.setText(text);
        tv.setBackgroundColor(bg);
        tv.setTextColor(fg);
    }

    private String safe(String s) { return s != null && !s.isEmpty() ? s : "--"; }

    @Override
    public int getItemCount() { return trips.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCarType, tvStatus, tvMode, tvRoute, tvCustomer, tvPrice;
        Button btnAction;

        VH(@NonNull View v) {
            super(v);
            tvCarType  = v.findViewById(R.id.tv_trip_cartype);
            tvStatus   = v.findViewById(R.id.tv_trip_status);
            tvMode     = v.findViewById(R.id.tv_trip_mode);
            tvRoute    = v.findViewById(R.id.tv_trip_route);
            tvCustomer = v.findViewById(R.id.tv_trip_customer);
            tvPrice    = v.findViewById(R.id.tv_trip_price);
            btnAction  = v.findViewById(R.id.btn_trip_action);
        }
    }
}
