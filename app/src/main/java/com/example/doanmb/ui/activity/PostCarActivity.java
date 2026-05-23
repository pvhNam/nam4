package com.example.doanmb.ui.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmb.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PostCarActivity extends AppCompatActivity {

    private EditText etCarName, etCarPrice, etCarInfo, etCarYear, etCarKm, etCarLocation;
    private Spinner spinnerBrand, spinnerType, spinnerFuel, spinnerCondition, spinnerTransmission;
    private Button btnSubmitPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_car);

        // Hiển thị nút quay lại trên thanh tiêu đề
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đăng tin mới");
        }

        initViews();
        setupSpinners();

        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void initViews() {
        etCarName = findViewById(R.id.et_car_name);
        etCarPrice = findViewById(R.id.et_car_price);
        etCarInfo = findViewById(R.id.et_car_info);
        etCarYear = findViewById(R.id.et_car_year);
        etCarKm = findViewById(R.id.et_car_km);
        etCarLocation = findViewById(R.id.et_car_location);
        spinnerBrand = findViewById(R.id.spinner_brand);
        spinnerType = findViewById(R.id.spinner_type);
        spinnerFuel = findViewById(R.id.spinner_fuel);
        spinnerCondition = findViewById(R.id.spinner_condition);
        spinnerTransmission = findViewById(R.id.spinner_transmission);
        btnSubmitPost = findViewById(R.id.btn_submit_post);
    }

    private void setupSpinners() {
        // Hãng xe
        String[] brands = {"Toyota", "Honda", "Mazda", "Kia", "Ford", "Hyundai", "VinFast", "Mitsubishi", "Suzuki", "Nissan", "Mercedes", "BMW", "Audi", "Khác"};
        spinnerBrand.setAdapter(makeAdapter(brands));

        // Loại hình
        String[] types = {"Cần Bán", "Cho Thuê"};
        spinnerType.setAdapter(makeAdapter(types));

        // Nhiên liệu
        String[] fuels = {"Xăng", "Điện", "Dầu Diesel", "Hybrid", "Xăng + Điện"};
        spinnerFuel.setAdapter(makeAdapter(fuels));

        // Tình trạng
        String[] conditions = {"Xe cũ đã qua sử dụng", "Xe mới 100%", "Xe mới đăng ký"};
        spinnerCondition.setAdapter(makeAdapter(conditions));

        // Hộp số
        String[] transmissions = {"Tự động", "Số sàn", "Bán tự động (CVT)"};
        spinnerTransmission.setAdapter(makeAdapter(transmissions));
    }

    private ArrayAdapter<String> makeAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void submitPost() {
        String name = etCarName.getText().toString().trim();
        String price = etCarPrice.getText().toString().trim();
        String info = etCarInfo.getText().toString().trim();
        String year = etCarYear.getText().toString().trim();
        String km = etCarKm.getText().toString().trim();
        String location = etCarLocation.getText().toString().trim();
        String brand = spinnerBrand.getSelectedItem().toString();
        String fuel = spinnerFuel.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();
        String transmission = spinnerTransmission.getSelectedItem().toString();
        String typeLabel = spinnerType.getSelectedItem().toString();
        String type = typeLabel.equals("Cần Bán") ? "sale" : "rental";

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên xe và giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo chuỗi thông tin tổng hợp
        String fullInfo = brand + " • " + fuel + " • " + transmission + " • " + condition
                + (year.isEmpty() ? "" : " • " + year)
                + (km.isEmpty() ? "" : " • " + km + " km")
                + (location.isEmpty() ? "" : " • " + location)
                + (info.isEmpty() ? "" : "\n" + info);

        // Lấy thông tin người dùng rồi lưu xe
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String sellerName = doc.getString("name");
                    String sellerPhone = doc.getString("phone");

                    Map<String, Object> car = new HashMap<>();
                    car.put("name", name);
                    car.put("price", price + (type.equals("rental") ? " đ/ngày" : " VNĐ"));
                    car.put("info", fullInfo);
                    car.put("type", type);
                    car.put("brand", brand);
                    car.put("fuel", fuel);
                    car.put("condition", condition);
                    car.put("transmission", transmission);
                    car.put("year", year);
                    car.put("km", km);
                    car.put("location", location);
                    car.put("status", "pending");
                    car.put("userId", user.getUid());
                    car.put("sellerId", user.getUid());
                    car.put("sellerName", sellerName != null ? sellerName : "");
                    car.put("sellerPhone", sellerPhone != null ? sellerPhone : "");
                    car.put("createdAt", com.google.firebase.Timestamp.now());

                    FirebaseFirestore.getInstance().collection("cars").add(car)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Đăng tin thành công! Tin đang chờ admin duyệt.", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi lấy thông tin người dùng!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}