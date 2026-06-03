package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.adapter.OrderHistoryAdapter;
import com.example.doanmb.adapter.ProfileCarAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.activity.LoginActivity;
import com.example.doanmb.ui.activity.RegisterActivity;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.app.DatePickerDialog;
import android.widget.EditText;
import java.util.Calendar;
public class ProfileFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn;
    private FrameLayout layoutLoggedIn;

    // 3 Sub-screens quản lý layer giao diện
    private ScrollView layoutMainProfile;
    private ConstraintLayout layoutAccountSettings;
    private RelativeLayout layoutPersonalInfo;

    // Views màn 1
    private CardView cardUserProfile;
    private TextView tvProfileNameMain, tvPhoneVerifiedBadge;
    private ImageView ivAvatarMain,ivVerifiedIcon,ivFavouriteCar,ivRegRentCar,ivLocation;
    private Button btnLogin, btnRegister, btnLogout;
    private TextView tvProfileName, tvProfilePhone;
    private ImageView ivAvatar;

    // Launcher chọn ảnh từ thư viện
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    // Tab 1: Xe đã đăng
    private Button btnFilterAll, btnFilterSale, btnFilterRental;
    private TextView tvCarCount, tvEmptyCars;
    private RecyclerView rvMyCars;
    private ProfileCarAdapter carAdapter;
    private List<Car> allCars = new ArrayList<>();
    private String currentFilter = "all";

    // Tab 2: Lịch sử đơn
    private TextView tvOrderCount, tvEmptyOrders;
    private RecyclerView rvMyOrders;
    private OrderHistoryAdapter orderAdapter;
    private List<Map<String, Object>> orderList = new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupListeners();
        return view;

    }

    private void initViews(View view) {
        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogout = view.findViewById(R.id.btn_logout);
        cardUserProfile = view.findViewById(R.id.card_user_profile);
        tvProfileNameMain = view.findViewById(R.id.tv_profile_name_main);
        tvPhoneVerifiedBadge = view.findViewById(R.id.tv_phone_verified_badge);
        ivAvatarMain = view.findViewById(R.id.iv_avatar_main);

        ivAvatarSettings = view.findViewById(R.id.iv_avatar_settings);
        ivChangeAvatarTrigger = view.findViewById(R.id.iv_change_avatar_trigger);
        menuPersonalInfoClick = view.findViewById(R.id.menu_personal_info);
        btnBackToMain = view.findViewById(R.id.btn_back_to_main);

        btnBackToSettings = view.findViewById(R.id.btn_back_to_settings);
        edtInfoName = view.findViewById(R.id.edt_info_name);
        edtInfoDob = view.findViewById(R.id.edt_info_dob);
        edtInfoGender = view.findViewById(R.id.edt_info_gender);
        edtInfoPhone = view.findViewById(R.id.edt_info_phone);
        btnSavePersonalInfo = view.findViewById(R.id.btn_save_personal_info);
        ivVerifiedIcon = view.findViewById(R.id.iv_verified_icon);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        cardUserProfile.setOnClickListener(v -> switchSubScreen(2));
        btnBackToMain.setOnClickListener(v -> switchSubScreen(1));
        menuPersonalInfoClick.setOnClickListener(v -> switchSubScreen(3));
        btnBackToSettings.setOnClickListener(v -> switchSubScreen(2));

        ivChangeAvatarTrigger.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSavePersonalInfo.setOnClickListener(v -> saveUserInformationToFirestore());
        edtInfoDob.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view1, selectedYear, selectedMonth, selectedDay) -> {
                        // Tự động thêm số 0 nếu ngày hoặc tháng < 10 (Ví dụ: 26/08/2005)
                        String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        edtInfoDob.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void showTab(boolean showCars) {
        if (showCars) {
            layoutTabMyCars.setVisibility(View.VISIBLE);
            layoutTabMyOrders.setVisibility(View.GONE);
            btnTabMyCars.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabMyCars.setTextColor(0xFFFFFFFF);
            btnTabMyOrders.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabMyOrders.setTextColor(0xFF1976D2);
        } else {
            layoutTabMyCars.setVisibility(View.GONE);
            layoutTabMyOrders.setVisibility(View.VISIBLE);
            btnTabMyOrders.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabMyOrders.setTextColor(0xFFFFFFFF);
            btnTabMyCars.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabMyCars.setTextColor(0xFF1976D2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            layoutNotLoggedIn.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            loadUserInfo(user);
            loadMyCars(user.getUid());
            loadMyOrders(user.getUid());
        } else {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void loadUserInfo(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(requireActivity(), doc -> {
                    if (!isAdded() || !doc.exists()) return;

                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String dob = doc.getString("dob");
                    String gender = doc.getString("gender");
                    String avatarUrl = doc.getString("avatarUrl");
                    Boolean phoneVerified = doc.getBoolean("phoneVerified");

                    tvProfileNameMain.setText(name != null ? name : "Chưa đặt tên");
                    if (phoneVerified != null && phoneVerified) {
                        tvPhoneVerifiedBadge.setText("Đã xác thực");
                        tvPhoneVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                        ivVerifiedIcon.setVisibility(View.VISIBLE); // Hiện icon tích xanh xịn sò lên
                    } else {
                        tvPhoneVerifiedBadge.setText("Chưa xác thực số điện thoại");
                        tvPhoneVerifiedBadge.setTextColor(android.graphics.Color.parseColor("#E53935"));
                        ivVerifiedIcon.setVisibility(View.GONE); // Ẩn icon đi khi chưa xác thực
                    }

                    edtInfoName.setText(name != null ? name : "");
                    edtInfoPhone.setText(phone != null ? phone : "");
                    edtInfoDob.setText(dob != null ? dob : "");
                    edtInfoGender.setText(gender != null ? gender : "Nam");

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        if (ivAvatarMain != null) {
                            Glide.with(ProfileFragment.this).load(avatarUrl).circleCrop().into(ivAvatarMain);
                        }
                        if (ivAvatarSettings != null) {
                            Glide.with(ProfileFragment.this).load(avatarUrl).circleCrop().into(ivAvatarSettings);
                        }
                    }
                });
    }

    // Upload ảnh đại diện lên Cloudinary rồi lưu URL vào Firestore
    private void uploadAvatar(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

        CloudinaryHelper.uploadImage(requireContext(), uri, new CloudinaryHelper.OnUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                if (!isAdded() || getActivity() == null) return;

                // Lưu URL vào Firestore
                Map<String, Object> update = new HashMap<>();
                update.put("avatarUrl", imageUrl);
                db.collection("users").document(user.getUid())
                        .update(update)
                        .addOnSuccessListener(requireActivity(), unused -> {
                            if (!isAdded()) return;

                            // SỬA LỖI GLIDE Ở ĐÂY: Thay getViewLifecycleOwner() bằng ProfileFragment.this
                            Glide.with(ProfileFragment.this)
                                    .load(imageUrl)
                                    .circleCrop()
                                    .into(ivAvatar);
                            Toast.makeText(getContext(), "✅ Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(String error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMyCars(String userId) {
        db.collection("cars").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(requireActivity(), snapshots -> {
                    if (!isAdded()) return;
                    allCars.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        if (name == null) continue;
                        Car car = new Car(name, price != null ? price : "", info != null ? info : "", android.R.drawable.ic_menu_gallery);
                        car.setId(doc.getId());
                        car.setType(type != null ? type : "");
                        car.setImageUrl(doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "");
                        allCars.add(car);
                    }
                    applyFilter(currentFilter);
                });
    }

    private void loadMyOrders(String userId) {
        db.collection("orders").whereEqualTo("buyerId", userId).get()
                .addOnSuccessListener(requireActivity(), snapshots -> {
                    if (!isAdded()) return;
                    orderList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        orderList.add(doc.getData());
                    }
                    orderAdapter.updateList(orderList);
                    tvOrderCount.setText("Đơn của tôi: " + orderList.size());
                    tvEmptyOrders.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvMyOrders.setVisibility(orderList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        int active = Color.parseColor("#1976D2");
        int inactive = Color.parseColor("#90A4AE");
        btnFilterAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("all") ? active : inactive));
        btnFilterSale.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("sale") ? active : inactive));
        btnFilterRental.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("rental") ? active : inactive));

        List<Car> filtered = new ArrayList<>();
        for (Car car : allCars) {
            String type = car.getType() != null ? car.getType().toLowerCase() : "";
            if (filter.equals("all") || filter.equals(type)) {
                filtered.add(car);
            }
        }

        carAdapter.updateList(filtered);
        tvCarCount.setText("Tin đã đăng: " + filtered.size());
        tvEmptyCars.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvMyCars.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }
}