package com.example.doanmb.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PostCarActivity extends AppCompatActivity {

    private EditText etCarName, etCarPrice, etCarInfo, etCarYear, etCarKm, etCarLocation;
    private Spinner spinnerBrand, spinnerType, spinnerFuel, spinnerCondition, spinnerTransmission;
    private Button btnSubmitPost, btnPickImage, btnRemoveImage;
    private ImageView ivPreview;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(ivPreview);
                    ivPreview.setVisibility(View.VISIBLE);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                }
            });
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền để chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_car);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đăng tin mới");
        }

        initViews();
        setupSpinners();

        btnPickImage.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // Android 13+
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else { // Android 12 trở xuống
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                }
            }
        });

        // Fix 4: nút xóa ảnh đã chọn
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri = null;
            ivPreview.setImageDrawable(null);
            ivPreview.setVisibility(View.GONE);
            btnRemoveImage.setVisibility(View.GONE);
        });

        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void initViews() {
        etCarName           = findViewById(R.id.et_car_name);
        etCarPrice          = findViewById(R.id.et_car_price);
        etCarInfo           = findViewById(R.id.et_car_info);
        etCarYear           = findViewById(R.id.et_car_year);
        etCarKm             = findViewById(R.id.et_car_km);
        etCarLocation       = findViewById(R.id.et_car_location);
        spinnerBrand        = findViewById(R.id.spinner_brand);
        spinnerType         = findViewById(R.id.spinner_type);
        spinnerFuel         = findViewById(R.id.spinner_fuel);
        spinnerCondition    = findViewById(R.id.spinner_condition);
        spinnerTransmission = findViewById(R.id.spinner_transmission);
        btnSubmitPost       = findViewById(R.id.btn_submit_post);
        btnPickImage        = findViewById(R.id.btn_pick_image);
        btnRemoveImage      = findViewById(R.id.btn_remove_image);
        ivPreview           = findViewById(R.id.iv_car_preview);
    }

    private void setupSpinners() {
        String[] brands = {"Toyota","Honda","Mazda","Kia","Ford","Hyundai","VinFast","Mitsubishi","Suzuki","Nissan","Mercedes","BMW","Audi","Khác"};
        spinnerBrand.setAdapter(makeAdapter(brands));
        String[] types = {"Cần Bán","Cho Thuê"};
        spinnerType.setAdapter(makeAdapter(types));
        String[] fuels = {"Xăng","Điện","Dầu Diesel","Hybrid","Xăng + Điện"};
        spinnerFuel.setAdapter(makeAdapter(fuels));
        String[] conditions = {"Xe cũ đã qua sử dụng","Xe mới 100%","Xe mới đăng ký"};
        spinnerCondition.setAdapter(makeAdapter(conditions));
        String[] transmissions = {"Tự động","Số sàn","Bán tự động (CVT)"};
        spinnerTransmission.setAdapter(makeAdapter(transmissions));
    }

    private ArrayAdapter<String> makeAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void submitPost() {
        String name         = etCarName.getText().toString().trim();
        String price        = etCarPrice.getText().toString().trim();
        String info         = etCarInfo.getText().toString().trim();
        String year         = etCarYear.getText().toString().trim();
        String km           = etCarKm.getText().toString().trim();
        String location     = etCarLocation.getText().toString().trim();
        String brand        = spinnerBrand.getSelectedItem().toString();
        String fuel         = spinnerFuel.getSelectedItem().toString();
        String condition    = spinnerCondition.getSelectedItem().toString();
        String transmission = spinnerTransmission.getSelectedItem().toString();
        String typeLabel    = spinnerType.getSelectedItem().toString();
        String type         = typeLabel.equals("Cần Bán") ? "sale" : "rental";

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên xe và giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập trước!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullInfo = brand + " • " + fuel + " • " + transmission + " • " + condition
                + (year.isEmpty()     ? "" : " • " + year)
                + (km.isEmpty()       ? "" : " • " + km + " km")
                + (location.isEmpty() ? "" : " • " + location)
                + (info.isEmpty()     ? "" : "\n" + info);

        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText("Đang đăng...");

        if (selectedImageUri != null) {
            // Fix 3: dùng getApplicationContext() tránh memory leak
            CloudinaryHelper.uploadImage(
                    getApplicationContext(),
                    selectedImageUri,
                    new CloudinaryHelper.OnUploadCallback() { // Fix 1: đúng tên interface
                        @Override
                        public void onSuccess(String imageUrl) {
                            saveCar(user, name, price, fullInfo, type, brand,
                                    fuel, condition, transmission, year, km, location, imageUrl);
                        }
                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(PostCarActivity.this,
                                    "Lỗi upload ảnh: " + error, Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    }
            );
        } else {
            saveCar(user, name, price, fullInfo, type, brand,
                    fuel, condition, transmission, year, km, location, "");
        }
    }

    private void saveCar(FirebaseUser user, String name, String price, String fullInfo,
                         String type, String brand, String fuel, String condition,
                         String transmission, String year, String km,
                         String location, String imageUrl) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String sellerName  = doc.getString("name");
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
                    car.put("imageUrl", imageUrl);
                    car.put("userId", user.getUid());
                    car.put("sellerId", user.getUid());
                    car.put("sellerName",  sellerName  != null ? sellerName  : "");
                    car.put("sellerPhone", sellerPhone != null ? sellerPhone : "");
                    car.put("createdAt", com.google.firebase.Timestamp.now());

                    FirebaseFirestore.getInstance().collection("cars").add(car)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Đăng tin thành công! Tin đang chờ admin duyệt.", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi lấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        btnSubmitPost.setEnabled(true);
        btnSubmitPost.setText("GỬI YÊU CẦU ĐĂNG TIN");
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}