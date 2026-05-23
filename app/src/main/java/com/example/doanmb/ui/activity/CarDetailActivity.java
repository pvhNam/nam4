package com.example.doanmb.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.doanmb.R;
import com.example.doanmb.model.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CarDetailActivity extends AppCompatActivity {

    private ImageView ivCarDetail;
    private TextView tvCarName, tvCarPrice, tvCarInfo;
    private TextView tvCarTypeBadge, tvCarFuelBadge, tvCarConditionBadge;
    private TextView tvSellerName, tvSellerPhone;

    // Form mua xe
    private LinearLayout layoutBuyForm;
    private EditText etBuyerNote;
    private Button btnSendRequest, btnCallSeller;

    // Form thuê xe
    private LinearLayout layoutRentForm;
    private EditText etRenterName, etRenterPhone, etRenterCCCD;
    private EditText etRentStartDate, etRentDays, etRenterNote;
    private Button btnSendRentRequest, btnCallRentSeller;

    private TextView tvReportCar;
    private FirebaseFirestore db;
    private String sellerPhone = "";
    private Car car;
    private String carId, sellerId, carType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        db = FirebaseFirestore.getInstance();

        initViews();

        car = (Car) getIntent().getSerializableExtra("CAR_DATA");
        carId = getIntent().getStringExtra("CAR_ID");
        sellerId = getIntent().getStringExtra("SELLER_ID");
        carType = getIntent().getStringExtra("CAR_TYPE");

        if (car != null) {
            ivCarDetail.setImageResource(car.getImageResId());
            tvCarName.setText(car.getName());
            tvCarPrice.setText(car.getPrice());
            tvCarInfo.setText(car.getInfo());
        }

        // Load thông tin chi tiết xe từ Firestore
        if (carId != null && !carId.isEmpty()) {
            loadCarDetail(carId);
        } else {
            setupByType(carType);
        }

        // Load thông tin người bán
        if (sellerId != null && !sellerId.isEmpty()) {
            loadSellerInfo(sellerId);
        } else {
            tvSellerName.setText("👤  Chưa có thông tin");
            tvSellerPhone.setText("📞  Chưa có thông tin");
            setupByType(carType);
        }

        setupButtons();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết xe");
        }
    }

    private void initViews() {
        ivCarDetail = findViewById(R.id.ivCarDetail);
        tvCarName = findViewById(R.id.tvCarNameDetail);
        tvCarPrice = findViewById(R.id.tvCarPriceDetail);
        tvCarInfo = findViewById(R.id.tvCarInfoDetail);
        tvCarTypeBadge = findViewById(R.id.tvCarTypeBadge);
        tvCarFuelBadge = findViewById(R.id.tvCarFuelBadge);
        tvCarConditionBadge = findViewById(R.id.tvCarConditionBadge);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerPhone = findViewById(R.id.tvSellerPhone);
        tvReportCar = findViewById(R.id.tv_report_car);

        layoutBuyForm = findViewById(R.id.layoutBuyForm);
        etBuyerNote = findViewById(R.id.etBuyerNote);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        btnCallSeller = findViewById(R.id.btnCallSeller);

        layoutRentForm = findViewById(R.id.layoutRentForm);
        etRenterName = findViewById(R.id.etRenterName);
        etRenterPhone = findViewById(R.id.etRenterPhone);
        etRenterCCCD = findViewById(R.id.etRenterCCCD);
        etRentStartDate = findViewById(R.id.etRentStartDate);
        etRentDays = findViewById(R.id.etRentDays);
        etRenterNote = findViewById(R.id.etRenterNote);
        btnSendRentRequest = findViewById(R.id.btnSendRentRequest);
        btnCallRentSeller = findViewById(R.id.btnCallRentSeller);
    }

    private void loadCarDetail(String id) {
        db.collection("cars").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String fuel = doc.getString("fuel");
                    String condition = doc.getString("condition");
                    String type = doc.getString("type");
                    String sPhone = doc.getString("sellerPhone");
                    String sName = doc.getString("sellerName");

                    // Cập nhật badge
                    if (fuel != null) tvCarFuelBadge.setText(fuel);
                    if (condition != null) {
                        if (condition.contains("mới 100")) tvCarConditionBadge.setText("Xe mới");
                        else if (condition.contains("đăng ký")) tvCarConditionBadge.setText("Mới ĐK");
                        else tvCarConditionBadge.setText("Xe cũ");
                    }

                    // Nếu document có sẵn sellerName/Phone thì dùng luôn
                    if (sName != null && !sName.isEmpty())
                        tvSellerName.setText("👤  " + sName);
                    if (sPhone != null && !sPhone.isEmpty()) {
                        sellerPhone = sPhone;
                        tvSellerPhone.setText("📞  " + sellerPhone);
                    }

                    setupByType(type != null ? type : carType);
                });
    }

    private void loadSellerInfo(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    if (phone != null && !phone.isEmpty()) sellerPhone = phone;
                    tvSellerName.setText("👤  " + (name != null ? name : "Không rõ"));
                    tvSellerPhone.setText("📞  " + (sellerPhone.isEmpty() ? "Không rõ" : sellerPhone));
                });
    }

    private void setupByType(String type) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isOwner = currentUser != null && currentUser.getUid().equals(sellerId);

        if (isOwner) {
            layoutBuyForm.setVisibility(View.GONE);
            layoutRentForm.setVisibility(View.GONE);
            if (tvReportCar != null) tvReportCar.setVisibility(View.GONE);
            tvCarTypeBadge.setText("rental".equals(type) ? "Cho Thuê" : "Cần Bán");
            tvCarTypeBadge.setBackgroundColor("rental".equals(type) ? 0xFF1976D2 : 0xFF4CAF50);
            TextView tvOwnerNote = findViewById(R.id.tvOwnerNote);
            if (tvOwnerNote != null) tvOwnerNote.setVisibility(View.VISIBLE);
            return;
        }

        if (tvReportCar != null) {
            tvReportCar.setVisibility(View.VISIBLE);
            tvReportCar.setOnClickListener(v -> showReportDialog());
        }

        if ("rental".equals(type)) {
            tvCarTypeBadge.setText("Cho Thuê");
            tvCarTypeBadge.setBackgroundColor(0xFF1976D2);
            layoutBuyForm.setVisibility(View.GONE);
            layoutRentForm.setVisibility(View.VISIBLE);

            // Tự điền sẵn tên + SĐT người dùng đang đăng nhập
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                db.collection("users").document(user.getUid()).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                String phone = doc.getString("phone");
                                if (name != null && etRenterName.getText().toString().isEmpty())
                                    etRenterName.setText(name);
                                if (phone != null && etRenterPhone.getText().toString().isEmpty())
                                    etRenterPhone.setText(phone);
                            }
                        });
            }
        } else {
            tvCarTypeBadge.setText("Cần Bán");
            tvCarTypeBadge.setBackgroundColor(0xFF4CAF50);
            layoutBuyForm.setVisibility(View.VISIBLE);
            layoutRentForm.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        btnSendRequest.setOnClickListener(v -> sendBuyRequest());
        btnCallSeller.setOnClickListener(v -> callSeller());
        btnSendRentRequest.setOnClickListener(v -> sendRentRequest());
        btnCallRentSeller.setOnClickListener(v -> callSeller());
    }

    private void callSeller() {
        if (!sellerPhone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + sellerPhone));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Không có số điện thoại người bán!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBuyRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi yêu cầu!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        Map<String, Object> order = new HashMap<>();
        order.put("buyerId", user.getUid());
        order.put("carId", carId != null ? carId : "");
        order.put("carName", car != null ? car.getName() : "");
        order.put("carPrice", car != null ? car.getPrice() : "");
        order.put("type", "Mua xe");
        order.put("note", etBuyerNote.getText().toString().trim());
        order.put("status", "pending");
        order.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ Gửi yêu cầu mua xe thành công!\nNgười bán sẽ liên hệ bạn sớm.", Toast.LENGTH_LONG).show();
                    etBuyerNote.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendRentRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để gửi yêu cầu!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String renterName = etRenterName.getText().toString().trim();
        String renterPhone = etRenterPhone.getText().toString().trim();
        String cccd = etRenterCCCD.getText().toString().trim();
        String startDate = etRentStartDate.getText().toString().trim();
        String days = etRentDays.getText().toString().trim();
        String note = etRenterNote.getText().toString().trim();

        if (renterName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ và tên!", Toast.LENGTH_SHORT).show(); return;
        }
        if (renterPhone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại!", Toast.LENGTH_SHORT).show(); return;
        }
        if (cccd.isEmpty() || cccd.length() < 9) {
            Toast.makeText(this, "Vui lòng nhập đúng số CCCD/CMND (ít nhất 9 số)!", Toast.LENGTH_SHORT).show(); return;
        }
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập ngày bắt đầu thuê!", Toast.LENGTH_SHORT).show(); return;
        }
        if (days.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số ngày thuê!", Toast.LENGTH_SHORT).show(); return;
        }

        Map<String, Object> order = new HashMap<>();
        order.put("buyerId", user.getUid());
        order.put("carId", carId != null ? carId : "");
        order.put("carName", car != null ? car.getName() : "");
        order.put("carPrice", car != null ? car.getPrice() : "");
        order.put("type", "Thuê xe");
        order.put("renterName", renterName);
        order.put("renterPhone", renterPhone);
        order.put("cccd", cccd);
        order.put("startDate", startDate);
        order.put("days", days);
        order.put("note", note);
        order.put("status", "pending");
        order.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ Gửi yêu cầu thuê xe thành công!\nChủ xe sẽ liên hệ xác nhận sớm.", Toast.LENGTH_LONG).show();
                    etRenterCCCD.setText("");
                    etRentStartDate.setText("");
                    etRentDays.setText("");
                    etRenterNote.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showReportDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để báo cáo!", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] reasons = {
                "Thông tin sai lệch",
                "Xe không tồn tại",
                "Giá cả bất hợp lý",
                "Hình ảnh không đúng thực tế",
                "Lừa đảo / Gian lận",
                "Nội dung không phù hợp"
        };
        final int[] selectedReason = {0};

        new AlertDialog.Builder(this)
                .setTitle("Báo cáo tin đăng này")
                .setSingleChoiceItems(reasons, 0, (dialog, which) -> selectedReason[0] = which)
                .setPositiveButton("Gửi báo cáo", (dialog, which) -> submitReport(currentUser, reasons[selectedReason[0]]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitReport(FirebaseUser currentUser, String reason) {
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String reporterName = userDoc.getString("name");
                    String carName = car != null ? car.getName() : "";

                    java.util.Map<String, Object> report = new java.util.HashMap<>();
                    report.put("reporterId", currentUser.getUid());
                    report.put("reporterName", reporterName != null ? reporterName : "Ẩn danh");
                    report.put("targetId", carId != null ? carId : "");
                    report.put("targetName", carName);
                    report.put("targetType", "car");
                    report.put("reason", reason);
                    report.put("status", "pending");
                    report.put("createdAt", com.google.firebase.Timestamp.now());

                    db.collection("reports").add(report)
                            .addOnSuccessListener(ref ->
                                    Toast.makeText(this, "✅ Đã gửi báo cáo. Chúng tôi sẽ xem xét sớm!", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}