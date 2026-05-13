package com.example.doanmb;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CarDetailActivity extends AppCompatActivity {

    private ImageView ivCarDetail;
    private TextView tvCarName, tvCarPrice, tvCarInfo, tvSelectedDates, tvCarTypeDetail;
    private LinearLayout layoutSaleActions, layoutRentalActions;
    private Button btnPickDate, btnRentNow, btnContactSeller, btnBuyNow;

    private Long startDateMs = null;
    private Long endDateMs   = null;
    private Car currentCar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();

        currentCar = (Car) getIntent().getSerializableExtra("CAR_DATA");
        if (currentCar != null) setupCarData(currentCar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết xe");
        }
    }

    private void initViews() {
        ivCarDetail         = findViewById(R.id.ivCarDetail);
        tvCarName           = findViewById(R.id.tvCarNameDetail);
        tvCarPrice          = findViewById(R.id.tvCarPriceDetail);
        tvCarInfo           = findViewById(R.id.tvCarInfoDetail);
        tvCarTypeDetail     = findViewById(R.id.tvCarTypeDetail);
        layoutSaleActions   = findViewById(R.id.layout_sale_actions);
        layoutRentalActions = findViewById(R.id.layout_rental_actions);
        tvSelectedDates     = findViewById(R.id.tvSelectedDates);
        btnPickDate         = findViewById(R.id.btnPickDate);
        btnRentNow          = findViewById(R.id.btnRentNow);
        btnContactSeller    = findViewById(R.id.btnContactSeller);
        btnBuyNow           = findViewById(R.id.btnBuyNow);
    }

    private void setupCarData(Car car) {
        ivCarDetail.setImageResource(android.R.drawable.ic_menu_gallery);
        tvCarName.setText(car.getName());
        tvCarPrice.setText(car.getPrice());
        tvCarInfo.setText(car.getInfo());

        String type    = car.getType() != null ? car.getType().toLowerCase() : "";
        boolean isSale = type.equals("sale") || type.contains("bán") || type.contains("ban");

        if (tvCarTypeDetail != null) {
            tvCarTypeDetail.setText(isSale ? "Cần bán" : "Cho thuê");
            tvCarTypeDetail.setBackgroundColor(isSale
                    ? android.graphics.Color.parseColor("#1976D2")
                    : android.graphics.Color.parseColor("#4CAF50"));
        }

        if (isSale) {
            layoutSaleActions.setVisibility(View.VISIBLE);
            layoutRentalActions.setVisibility(View.GONE);
            setupSaleActions(car);
        } else {
            layoutSaleActions.setVisibility(View.GONE);
            layoutRentalActions.setVisibility(View.VISIBLE);
            setupRentalActions(car);
        }
    }

    // thuê xe

    private void setupRentalActions(Car car) {
        btnRentNow.setEnabled(false);

        btnPickDate.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker =
                    MaterialDatePicker.Builder.dateRangePicker()
                            .setTitleText("Chọn ngày nhận và trả xe")
                            .build();

            picker.addOnPositiveButtonClickListener(selection -> {
                startDateMs = selection.first;
                endDateMs   = selection.second;

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String start = sdf.format(new Date(startDateMs));
                String end   = sdf.format(new Date(endDateMs));
                long days    = ((endDateMs - startDateMs) / (1000 * 60 * 60 * 24)) + 1;

                tvSelectedDates.setText("Từ: " + start + " → Đến: " + end + " (" + days + " ngày)");
                btnRentNow.setEnabled(true);
            });

            picker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        btnRentNow.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }
            showRentalDialog(car, user);
        });
    }

    private void showRentalDialog(Car car, FirebaseUser user) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String start = sdf.format(new Date(startDateMs));
        String end   = sdf.format(new Date(endDateMs));
        long days    = ((endDateMs - startDateMs) / (1000 * 60 * 60 * 24)) + 1;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_form, null);
        EditText etCccd = dialogView.findViewById(R.id.et_cccd);
        EditText etNote = dialogView.findViewById(R.id.et_note);

        TextView tvDialogInfo = dialogView.findViewById(R.id.tv_dialog_info);
        tvDialogInfo.setText("Xe: " + car.getName()
                + "\nGiá: " + car.getPrice()
                + "\nTừ: " + start + " → " + end
                + "\nSố ngày: " + days + " ngày");

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đặt thuê xe")
                .setView(dialogView)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String cccd = etCccd.getText().toString().trim();
                    String note = etNote.getText().toString().trim();
                    if (cccd.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập CCCD!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitOrder(car, user, start, days, cccd, note, "Thuê xe");
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // Mua xe

    private void setupSaleActions(Car car) {
        btnContactSeller.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng chat đang phát triển!", Toast.LENGTH_SHORT).show());

        btnBuyNow.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (user.getUid().equals(car.getUserId())) {
                Toast.makeText(this, "Bạn không thể mua xe của chính mình!", Toast.LENGTH_SHORT).show();
                return;
            }
            showBuyDialog(car, user);
        });
    }

    private void showBuyDialog(Car car, FirebaseUser user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_form, null);
        EditText etCccd   = dialogView.findViewById(R.id.et_cccd);
        EditText etNote   = dialogView.findViewById(R.id.et_note);
        TextView tvDialogInfo = dialogView.findViewById(R.id.tv_dialog_info);

        tvDialogInfo.setText("Xe: " + car.getName() + "\nGiá: " + car.getPrice());

        // Ẩn field ngày
        View layoutDays = dialogView.findViewById(R.id.layout_days_hint);
        if (layoutDays != null) layoutDays.setVisibility(View.GONE);

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận mua xe")
                .setView(dialogView)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String cccd = etCccd.getText().toString().trim();
                    String note = etNote.getText().toString().trim();
                    if (cccd.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập CCCD!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    submitOrder(car, user, null, 0, cccd, note, "Mua xe");
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // thêm vào firebase

    private void submitOrder(Car car, FirebaseUser user,
                             String startDate, long days,
                             String cccd, String note, String type) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String renterName  = doc.getString("name")  != null ? doc.getString("name")  : "";
                    String renterPhone = doc.getString("phone") != null ? doc.getString("phone") : "";

                    Map<String, Object> order = new HashMap<>();
                    order.put("buyerId",     user.getUid());
                    order.put("carId",       car.getCarId()  != null ? car.getCarId()  : "");
                    order.put("carName",     car.getName());
                    order.put("carPrice",    car.getPrice());
                    order.put("cccd",        cccd);
                    order.put("createdAt",   com.google.firebase.Timestamp.now());
                    order.put("days",        days > 0 ? String.valueOf(days) : "");
                    order.put("note",        note);
                    order.put("renterName",  renterName);
                    order.put("renterPhone", renterPhone);
                    order.put("startDate",   startDate != null ? startDate : "");
                    order.put("status",      "pending");
                    order.put("type",        type);

                    db.collection("orders").add(order)
                            .addOnSuccessListener(ref -> {
                                if (type.equals("Thuê xe")) {
                                    btnRentNow.setEnabled(false);
                                    tvSelectedDates.setText("Đã đặt thuê thành công ✓");
                                    Toast.makeText(this,
                                            "Đặt thuê xe thành công! Chờ chủ xe xác nhận.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    btnBuyNow.setEnabled(false);
                                    btnBuyNow.setText("Đã gửi yêu cầu ✓");
                                    Toast.makeText(this,
                                            "Gửi yêu cầu mua xe thành công!",
                                            Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi lấy thông tin: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}