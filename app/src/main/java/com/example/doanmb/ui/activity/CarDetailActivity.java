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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.adapter.CarImageAdapter;
import com.example.doanmb.model.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarDetailActivity extends AppCompatActivity {

    private ImageView ivCarDetail;
    private RecyclerView rvCarImages;
    private LinearLayout layoutImageDots;
    private CarImageAdapter imageAdapter;
    private TextView tvCarName, tvCarPrice, tvCarInfo;
    private TextView tvCarTypeBadge, tvCarFuelBadge, tvCarConditionBadge;
    private TextView tvSellerName, tvSellerPhone, tvOwnerNote;

    // Form mua xe
    private LinearLayout layoutBuyForm;
    private EditText etBuyerNote;
    private Button btnSendRequest, btnCallSeller, btnChatSeller;

    // Form thuê xe
    private LinearLayout layoutRentForm;
    private EditText etRenterName, etRenterPhone, etRenterCCCD;
    private EditText etRentStartDate, etRentDays, etRenterNote;
    private Button btnSendRentRequest, btnCallRentSeller, btnChatRentSeller;

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
        setupDetailHeader();

        car = (Car) getIntent().getSerializableExtra("CAR_DATA");
        carId = getIntent().getStringExtra("CAR_ID");
        sellerId = getIntent().getStringExtra("SELLER_ID");
        carType = getIntent().getStringExtra("CAR_TYPE");

        tvCarFuelBadge.setVisibility(View.GONE);
        tvCarConditionBadge.setVisibility(View.GONE);

        if (car != null) {
            // Hiển thị tạm ảnh đại diện; loadCarDetail() sẽ nạp đủ ảnh (vuốt) từ Firestore
            List<String> coverImages = new ArrayList<>();
            if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
                coverImages.add(car.getImageUrl());
            }
            showImages(coverImages);
            tvCarName.setText(car.getName());
            tvCarPrice.setText(car.getPrice());
            tvCarInfo.setText(car.getInfo());
        }

        if ((carId == null || carId.isEmpty()) && car != null && car.getId() != null) {
            carId = car.getId();
        }
        if ((sellerId == null || sellerId.isEmpty()) && car != null && car.getSellerId() != null) {
            sellerId = car.getSellerId();
        }

        if (carId != null && !carId.isEmpty()) {
            loadCarDetail(carId);
        }

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
        ivCarDetail       = findViewById(R.id.ivCarDetail);
        rvCarImages       = findViewById(R.id.rv_car_images);
        layoutImageDots   = findViewById(R.id.layout_image_dots);
        setupImagePager();
        tvCarName         = findViewById(R.id.tvCarNameDetail);
        tvCarPrice        = findViewById(R.id.tvCarPriceDetail);
        tvCarInfo         = findViewById(R.id.tvCarInfoDetail);
        tvCarTypeBadge    = findViewById(R.id.tvCarTypeBadge);
        tvCarFuelBadge    = findViewById(R.id.tvCarFuelBadge);
        tvCarConditionBadge = findViewById(R.id.tvCarConditionBadge);
        tvSellerName = findViewById(R.id.tvSellerName);
        tvSellerPhone = findViewById(R.id.tvSellerPhone);
        tvReportCar = findViewById(R.id.tv_report_car);
        tvOwnerNote = findViewById(R.id.tvOwnerNote);

        layoutBuyForm = findViewById(R.id.layoutBuyForm);
        etBuyerNote = findViewById(R.id.etBuyerNote);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        btnCallSeller = findViewById(R.id.btnCallSeller);
        btnChatSeller = findViewById(R.id.btnChatSeller);

        layoutRentForm = findViewById(R.id.layoutRentForm);
        etRenterName = findViewById(R.id.etRenterName);
        etRenterPhone = findViewById(R.id.etRenterPhone);
        etRenterCCCD = findViewById(R.id.etRenterCCCD);
        etRentStartDate = findViewById(R.id.etRentStartDate);
        etRentDays = findViewById(R.id.etRentDays);
        etRenterNote = findViewById(R.id.etRenterNote);
        btnSendRentRequest = findViewById(R.id.btnSendRentRequest);
        btnCallRentSeller  = findViewById(R.id.btnCallRentSeller);
        btnChatRentSeller = findViewById(R.id.btnChatRentSeller);
    }

    private void setupDetailHeader() {
        View btnBack = findViewById(R.id.btn_back_detail);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View header = findViewById(R.id.header_detail);
        if (header != null) {
            final int baseTop = header.getPaddingTop();
            // Đẩy header xuống dưới thanh trạng thái để ảnh không bị che (edge-to-edge)
            ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
                int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), baseTop + top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void setupImagePager() {
        if (rvCarImages == null) return;
        imageAdapter = new CarImageAdapter();
        rvCarImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCarImages.setAdapter(imageAdapter);
        new PagerSnapHelper().attachToRecyclerView(rvCarImages);

        rvCarImages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int pos = lm.findFirstCompletelyVisibleItemPosition();
                if (pos == RecyclerView.NO_POSITION) pos = lm.findFirstVisibleItemPosition();
                updateDots(pos);
            }
        });
    }

    /** Đổ danh sách ảnh vào pager và dựng chấm chỉ số. */
    private void showImages(List<String> images) {
        if (imageAdapter == null) return;
        imageAdapter.setImages(images);
        buildDots(images.size());
        updateDots(0);
    }

    private void buildDots(int count) {
        if (layoutImageDots == null) return;
        layoutImageDots.removeAllViews();
        if (count <= 1) return; // 1 ảnh thì không cần chấm

        int size = Math.round(7 * getResources().getDisplayMetrics().density);
        int margin = Math.round(3 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_image_dot);
            layoutImageDots.addView(dot);
        }
    }

    private void updateDots(int activePos) {
        if (layoutImageDots == null) return;
        for (int i = 0; i < layoutImageDots.getChildCount(); i++) {
            View dot = layoutImageDots.getChildAt(i);
            dot.setAlpha(i == activePos ? 1f : 0.4f);
            dot.setScaleX(i == activePos ? 1.25f : 1f);
            dot.setScaleY(i == activePos ? 1.25f : 1f);
        }
    }

    /** Chuyển field "imageUrls" của Firestore (List) thành List<String> an toàn. */
    private static List<String> extractImageUrls(Object raw) {
        List<String> urls = new ArrayList<>();
        if (raw instanceof List) {
            for (Object item : (List<?>) raw) {
                if (item != null && !item.toString().isEmpty()) urls.add(item.toString());
            }
        }
        return urls;
    }

    private void setupButtons() {
        btnSendRequest.setOnClickListener(v -> sendBuyRequest());
        btnCallSeller.setOnClickListener(v -> callSeller());
        btnChatSeller.setOnClickListener(v -> openChat());
        
        btnSendRentRequest.setOnClickListener(v -> sendRentRequest());
        btnCallRentSeller.setOnClickListener(v -> callSeller());
        btnChatRentSeller.setOnClickListener(v -> openChat());
    }

    private void openChat() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để nhắn tin!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        if (sellerId == null || sellerId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người bán", Toast.LENGTH_SHORT).show();
            return;
        }

        String roomId = user.getUid() + "_" + sellerId + "_" + (carId != null ? carId : "unknown");

        Map<String, Object> roomData = new HashMap<>();
        List<String> participants = new ArrayList<>();
        participants.add(user.getUid());
        participants.add(sellerId);
        
        roomData.put("participants", participants);
        roomData.put("carId", carId != null ? carId : "");
        roomData.put("carName", car != null ? car.getName() : "Xe");
        roomData.put("carPrice", car != null ? car.getPrice() : "");
        roomData.put("carImage", car != null ? car.getImageUrl() : "");
        roomData.put("carType", carType != null ? carType : "sale");
        roomData.put("buyerId", user.getUid());
        roomData.put("sellerId", sellerId);

        db.collection("chat_rooms").document(roomId)
                .set(roomData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, ChatDetailActivity.class);
                    intent.putExtra("ROOM_ID", roomId);
                    intent.putExtra("PARTNER_ID", sellerId);
                    intent.putExtra("PARTNER_NAME", tvSellerName.getText().toString().replace("👤  ", ""));
                    intent.putExtra("CAR_DATA", car);
                    startActivity(intent);
                });
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

    private void loadCarDetail(String id) {
        db.collection("cars").document(id).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String fuel      = doc.getString("fuel");
                    String condition = doc.getString("condition");
                    String type      = doc.getString("type");
                    String sPhone    = doc.getString("sellerPhone");
                    String sName     = doc.getString("sellerName");
                    String imageUrl  = doc.getString("imageUrl");

                    if (type != null) carType = type;

                    List<String> imgs = extractImageUrls(doc.get("imageUrls"));
                    if (imgs.isEmpty() && imageUrl != null && !imageUrl.isEmpty()) {
                        imgs.add(imageUrl);
                    }
                    if (!imgs.isEmpty()) {
                        showImages(imgs);
                    }

                    if (fuel != null && !fuel.isEmpty()) {
                        tvCarFuelBadge.setText(fuel);
                        tvCarFuelBadge.setVisibility(View.VISIBLE);
                    }

                    if (condition != null && !condition.isEmpty()) {
                        if (condition.contains("mới 100") || condition.equalsIgnoreCase("Xe mới 100%"))
                            tvCarConditionBadge.setText("Xe mới");
                        else
                            tvCarConditionBadge.setText("Xe cũ");
                        tvCarConditionBadge.setVisibility(View.VISIBLE);
                    }

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
                    String name  = doc.getString("name");
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
            if (tvOwnerNote != null) tvOwnerNote.setVisibility(View.VISIBLE);
            return;
        }

        if (tvOwnerNote != null) tvOwnerNote.setVisibility(View.GONE);
        if (tvReportCar != null) {
            tvReportCar.setVisibility(View.VISIBLE);
            tvReportCar.setOnClickListener(v -> showReportDialog());
        }

        if ("rental".equals(type)) {
            tvCarTypeBadge.setText("Cho Thuê");
            tvCarTypeBadge.setBackgroundColor(0xFF1976D2);
            layoutBuyForm.setVisibility(View.GONE);
            layoutRentForm.setVisibility(View.VISIBLE);
        } else {
            tvCarTypeBadge.setText("Cần Bán");
            tvCarTypeBadge.setBackgroundColor(0xFF4CAF50);
            layoutBuyForm.setVisibility(View.VISIBLE);
            layoutRentForm.setVisibility(View.GONE);
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
        order.put("buyerId",  user.getUid());
        order.put("sellerId", sellerId != null ? sellerId : "");
        order.put("carId",    carId != null ? carId : "");
        order.put("carName",  car != null ? car.getName() : "");
        order.put("carPrice", car != null ? car.getPrice() : "");
        order.put("type",     "Mua xe");
        order.put("note",     etBuyerNote.getText().toString().trim());
        order.put("status",   "pending");
        order.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ Gửi yêu cầu thành công!", Toast.LENGTH_LONG).show();
                    etBuyerNote.setText("");
                });
    }

    private void sendRentRequest() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> order = new HashMap<>();
        order.put("buyerId",    user.getUid());
        order.put("sellerId",   sellerId != null ? sellerId : "");
        order.put("carId",      carId != null ? carId : "");
        order.put("carName",    car != null ? car.getName() : "");
        order.put("status",     "pending");
        order.put("createdAt",  com.google.firebase.Timestamp.now());

        db.collection("orders").add(order)
                .addOnSuccessListener(ref -> Toast.makeText(this, "✅ Gửi yêu cầu thuê xe thành công!", Toast.LENGTH_LONG).show());
    }

    private void showReportDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        final String[] reasons = {"Thông tin sai lệch", "Xe không tồn tại", "Giá bất hợp lý", "Lừa đảo"};
        new AlertDialog.Builder(this)
                .setTitle("Báo cáo tin đăng")
                .setItems(reasons, (dialog, which) -> submitReport(currentUser, reasons[which]))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void submitReport(FirebaseUser currentUser, String reason) {
        Map<String, Object> report = new HashMap<>();
        report.put("reporterId", currentUser.getUid());
        report.put("targetId", carId);
        report.put("reason", reason);
        report.put("status", "pending");
        report.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("reports").add(report)
                .addOnSuccessListener(ref -> Toast.makeText(this, "✅ Đã gửi báo cáo!", Toast.LENGTH_LONG).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
