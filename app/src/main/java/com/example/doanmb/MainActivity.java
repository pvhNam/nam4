package com.example.doanmb;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.adapter.BannerAdapter;
import com.example.doanmb.ui.activity.CarDetailActivity;
import com.example.doanmb.ui.fragment.CategoryFragment;
import com.example.doanmb.ui.fragment.ManageFragment;
import com.example.doanmb.ui.fragment.MessagesFragment;
import com.example.doanmb.ui.fragment.ProfileFragment;
import com.example.doanmb.adapter.CarRentalAdapter;
import com.example.doanmb.adapter.CarSaleAdapter;
import com.example.doanmb.model.Car;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import com.cloudinary.android.MediaManager;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvFeaturedSales, rvRentalCars, rvBanners;
    private AutoCompleteTextView etSearch;
    private TextView tvGreeting;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout fragmentContainer;
    private View homeLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView nestedScroll;

    // Header & sticky bar
    private LinearLayout headerLayout;
    private LinearLayout stickyBar;

    // Quick action buttons
    private LinearLayout btnBuyCar, btnDriver, btnPoliceCar, btnPayment;

    private CarSaleAdapter carSaleAdapter;
    private CarRentalAdapter carRentalAdapter;

    private List<Car> saleCarList   = new ArrayList<>();
    private List<Car> rentalCarList = new ArrayList<>();

    private FirebaseFirestore db;

    // Ngưỡng scroll ~80dp để thu gọn header
    private static final int SCROLL_THRESHOLD_DP = 80;
    private int scrollThresholdPx;
    private boolean isStickyVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollThresholdPx = (int) (SCROLL_THRESHOLD_DP * getResources().getDisplayMetrics().density);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupBanners();
        loadUserGreeting();
        setupRecyclerViews();
        loadCarsFromFirestore();
        setupSearch();
        setupBottomNavigation();
        setupSwipeRefresh();
        setupQuickActions();
        setupScrollBehavior();
    }

    private void initViews() {
        rvFeaturedSales      = findViewById(R.id.rv_featured_sales);
        rvRentalCars         = findViewById(R.id.rv_new_rentals);
        rvBanners            = findViewById(R.id.rv_banners);
        etSearch             = findViewById(R.id.et_search);
        tvGreeting           = findViewById(R.id.tv_greeting);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentContainer    = findViewById(R.id.fragment_container);
        homeLayout           = findViewById(R.id.home_layout);
        swipeRefreshLayout   = findViewById(R.id.swipe_refresh);
        nestedScroll         = findViewById(R.id.nested_scroll);
        headerLayout         = findViewById(R.id.header_layout);
        stickyBar            = findViewById(R.id.sticky_bar);

        // Quick action buttons
        btnBuyCar    = findViewById(R.id.btn_buy_car);
        btnDriver    = findViewById(R.id.btn_driver);
        btnPoliceCar = findViewById(R.id.btn_police_car);
        btnPayment   = findViewById(R.id.btn_payment);


    }

    /**
     * Lắng nghe scroll:
     * - scroll lên > ngưỡng: ẩn header, hiện sticky bar
     * - scroll xuống < ngưỡng: ẩn sticky bar, hiện header
     */
    private void setupScrollBehavior() {
        if (nestedScroll == null) return;

        nestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY > scrollThresholdPx && !isStickyVisible) {
                        isStickyVisible = true;
                        showStickyBar();
                    } else if (scrollY <= scrollThresholdPx && isStickyVisible) {
                        isStickyVisible = false;
                        hideStickyBar();
                    }
                });
    }

    private void showStickyBar() {
        headerLayout.animate()
                .alpha(0f)
                .translationY(-30f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        headerLayout.setVisibility(View.GONE);
                        headerLayout.setAlpha(1f);
                        headerLayout.setTranslationY(0f);
                    }
                });

        stickyBar.setAlpha(0f);
        stickyBar.setTranslationY(-20f);
        stickyBar.setVisibility(View.VISIBLE);
        stickyBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setListener(null);
    }

    private void hideStickyBar() {
        stickyBar.animate()
                .alpha(0f)
                .translationY(-20f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        stickyBar.setVisibility(View.GONE);
                        stickyBar.setAlpha(1f);
                        stickyBar.setTranslationY(0f);
                    }
                });

        headerLayout.setAlpha(0f);
        headerLayout.setTranslationY(-30f);
        headerLayout.setVisibility(View.VISIBLE);
        headerLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .setListener(null);
    }

    private void setupQuickActions() {
        if (btnBuyCar != null)
            btnBuyCar.setOnClickListener(v ->
                    bottomNavigationView.setSelectedItemId(R.id.nav_cataloge));
        if (btnDriver != null)
            btnDriver.setOnClickListener(v ->
                    bottomNavigationView.setSelectedItemId(R.id.nav_cataloge));
        if (btnPoliceCar != null)
            btnPoliceCar.setOnClickListener(v ->
                    bottomNavigationView.setSelectedItemId(R.id.nav_cataloge));
        if (btnPayment != null)
            btnPayment.setOnClickListener(v ->
                    bottomNavigationView.setSelectedItemId(R.id.nav_manage));
    }

    private void setupBanners() {
        List<Integer> bannerList = new ArrayList<>();
        bannerList.add(R.drawable.banner_1);
        bannerList.add(R.drawable.banner_2);
        bannerList.add(R.drawable.banner_3);
        bannerList.add(R.drawable.banner_4);
        BannerAdapter bannerAdapter = new BannerAdapter(bannerList);
        rvBanners.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvBanners.setAdapter(bannerAdapter);
    }

    private void loadUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        de.hdodenhof.circleimageview.CircleImageView imgAvatar = findViewById(R.id.img_avatar);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String fullName = doc.getString("fullName");
                    if (fullName == null || fullName.isEmpty()) fullName = doc.getString("name");
                    if (fullName == null || fullName.isEmpty()) fullName = user.getDisplayName();
                    if (fullName != null && !fullName.isEmpty()) {
                        tvGreeting.setText("Hi, " + fullName);
                    }

                    String photoUrl = doc.getString("photoUrl");
                    if (photoUrl == null || photoUrl.isEmpty()) photoUrl = doc.getString("avatarUrl");
                    if ((photoUrl == null || photoUrl.isEmpty()) && user.getPhotoUrl() != null) {
                        photoUrl = user.getPhotoUrl().toString();
                    }

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        com.bumptech.glide.Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .circleCrop()
                                .into(imgAvatar);
                    }
                })
                .addOnFailureListener(e -> {
                    String name = user.getDisplayName();
                    if (name != null && !name.isEmpty()) tvGreeting.setText("Hi, " + name);
                    if (user.getPhotoUrl() != null) {
                        com.bumptech.glide.Glide.with(this)
                                .load(user.getPhotoUrl())
                                .placeholder(R.mipmap.ic_launcher_round)
                                .circleCrop()
                                .into(imgAvatar);
                    }
                });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            saleCarList.clear();
            rentalCarList.clear();
            loadCarsFromFirestore();
        });
    }

    private void setupRecyclerViews() {
        rvFeaturedSales.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRentalCars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        carSaleAdapter = new CarSaleAdapter(saleCarList, car -> openCarDetail(car));
        rvFeaturedSales.setAdapter(carSaleAdapter);

        carRentalAdapter = new CarRentalAdapter(rentalCarList, car -> openCarDetail(car));
        rvRentalCars.setAdapter(carRentalAdapter);
    }

    private void openCarDetail(Car car) {
        Intent intent = new Intent(MainActivity.this, CarDetailActivity.class);
        intent.putExtra("CAR_DATA", car);
        intent.putExtra("CAR_ID", car.getId());
        intent.putExtra("SELLER_ID", car.getSellerId());
        intent.putExtra("CAR_TYPE", car.getType());
        startActivity(intent);
    }

    private void loadCarsFromFirestore() {
        db.collection("cars").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    saleCarList.clear();
                    rentalCarList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name     = doc.getString("name");
                        String price    = doc.getString("price");
                        String info     = doc.getString("info");
                        String type     = doc.getString("type");
                        String sellerId = doc.getString("sellerId");
                        String status   = doc.getString("status");
                        String imageUrl = doc.getString("imageUrl");

                        if (name == null) continue;
                        if ("sold".equals(status)) continue;

                        Car car = new Car(name,
                                price    != null ? price    : "",
                                info     != null ? info     : "",
                                android.R.drawable.ic_menu_gallery);
                        car.setId(doc.getId());
                        car.setType(type     != null ? type     : "");
                        car.setImageUrl(imageUrl != null ? imageUrl : "");
                        car.setSellerId(sellerId != null ? sellerId : "");

                        if ("holding".equals(status)) {
                            Car holdingCar = new Car(
                                    "⏳ " + name,
                                    price != null ? price : "",
                                    "Đang có người đặt • " + (info != null ? info : ""),
                                    android.R.drawable.ic_menu_gallery);
                            holdingCar.setId(doc.getId());
                            holdingCar.setType(type     != null ? type     : "");
                            holdingCar.setImageUrl(imageUrl != null ? imageUrl : "");
                            holdingCar.setSellerId(sellerId != null ? sellerId : "");
                            car = holdingCar;
                        }

                        String normalizedType = type != null ? type.toLowerCase().trim() : "";
                        if (normalizedType.equals("sale"))        saleCarList.add(car);
                        else if (normalizedType.equals("rental")) rentalCarList.add(car);
                    }

                    carSaleAdapter.filterList(saleCarList);
                    carRentalAdapter.filterList(rentalCarList);
                    setupAutoComplete();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    loadDummyData();
                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadDummyData() {
        Car c1 = new Car("Toyota Vios 2020", "450.000.000 VNĐ", "TP.HCM • 2020 • Tự động", android.R.drawable.ic_menu_gallery);
        c1.setType("sale");
        saleCarList.add(c1);

        Car c2 = new Car("Hyundai Accent 2022", "800.000đ / ngày", "Quận 1, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery);
        c2.setType("rental");
        rentalCarList.add(c2);

        carSaleAdapter.filterList(saleCarList);
        carRentalAdapter.filterList(rentalCarList);
    }

    private void setupAutoComplete() {
        List<String> carNames = new ArrayList<>();
        for (Car car : saleCarList)   carNames.add(car.getName());
        for (Car car : rentalCarList) carNames.add(car.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, carNames);
        etSearch.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { filter(s.toString()); }
        });
    }

    private void filter(String text) {
        List<Car> filteredSale = new ArrayList<>();
        for (Car car : saleCarList)
            if (car.getName().toLowerCase().contains(text.toLowerCase()))
                filteredSale.add(car);
        carSaleAdapter.filterList(filteredSale);

        List<Car> filteredRental = new ArrayList<>();
        for (Car car : rentalCarList)
            if (car.getName().toLowerCase().contains(text.toLowerCase()))
                filteredRental.add(car);
        carRentalAdapter.filterList(filteredRental);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                homeLayout.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
                // Reset scroll về đầu khi về Home
                if (nestedScroll != null) nestedScroll.smoothScrollTo(0, 0);
                return true;
            } else if (itemId == R.id.nav_cataloge) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CategoryFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_manage) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ManageFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_messages) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MessagesFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_account) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment()).commit();
                return true;
            }
            return false;
        });
    }
}