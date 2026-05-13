package com.example.doanmb;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostCarActivity extends AppCompatActivity {

    private EditText etCarName, etCarPrice, etCarInfo;
    private Spinner spinnerBrand, spinnerType;
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

        // Xử lý nút submit lưu dữ liệu lên Firestore
        btnSubmitPost.setOnClickListener(v -> {
            String name = etCarName.getText().toString().trim();
            String price = etCarPrice.getText().toString().trim();
            String info = etCarInfo.getText().toString().trim();
            String brand = spinnerBrand.getSelectedItem().toString();
            String type = spinnerType.getSelectedItem().toString().equals("Cần Bán") ? "sale" : "rental";

            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên xe và giá!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lưu lên Firestore thật
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            java.util.Map<String, Object> car = new java.util.HashMap<>();
            car.put("name", name);
            car.put("price", price);
            car.put("info", info.isEmpty() ? brand : info);
            car.put("brand", brand);
            car.put("type", type);
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            car.put("userId", uid);

            db.collection("cars").add(car)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void initViews() {
        etCarName = findViewById(R.id.et_car_name);
        etCarPrice = findViewById(R.id.et_car_price);
        etCarInfo = findViewById(R.id.et_car_info);
        spinnerBrand = findViewById(R.id.spinner_brand);
        spinnerType = findViewById(R.id.spinner_type);
        btnSubmitPost = findViewById(R.id.btn_submit_post);
    }

    private void setupSpinners() {
        // Danh sách hãng xe
        String[] brands = {"Toyota", "Honda", "Mazda", "Kia", "Ford", "Hyundai", "VinFast", "Khác"};
        ArrayAdapter<String> brandAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, brands);
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBrand.setAdapter(brandAdapter);

        // Danh sách loại tin
        String[] types = {"Cần Bán", "Cho Thuê"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
