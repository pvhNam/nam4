package com.example.doanmb;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
    private LinearLayout stickyBtnBuyCar, stickyBtnDriver, stickyBtnPoliceCar, stickyBtnPayment;

    private CarSaleAdapter carSaleAdapter;
    private CarRentalAdapter carRentalAdapter;

    private List<Car> saleCarList   = new ArrayList<>();
    private List<Car> rentalCarList = new ArrayList<>();

    private FirebaseFirestore db;

    // Ngưỡng scroll: kéo lên một đoạn thì avatar/tên/search biến mất, các nút nằm trong khung xanh
    private static final int SCROLL_THRESHOLD_DP = 72;
    private int scrollThresholdPx;
    private boolean isStickyVisible = false;

    // Điều hướng Back: lưu lịch sử các tab đã đi qua để quay lại tab trước đó
    private int currentNavId = R.id.nav_home;
    private boolean isHandlingBack = false;
    private final Deque<Integer> navBackStack = new ArrayDeque<>();

    // Giữ lại fragment đã mở của từng tab để khi quay lại không phải dựng/tải lại từ đầu (tránh giật)
    private final java.util.Map<Integer, Fragment> fragmentCache = new java.util.HashMap<>();
    private Fragment activeFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        scrollThresholdPx = dp(SCROLL_THRESHOLD_DP);

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
        setupBackNavigation();
    }

    /**
     * Vuốt/nhấn Back: luôn quay về tab trước đó thay vì thoát app.
     * - Trong tab Danh mục đang mở form đăng xe → quay lại danh sách.
     * - Còn lịch sử tab → quay lại tab vừa rời đi.
     * - Đang ở Home và hết lịch sử → mới thoát app.
     */
    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!navBackStack.isEmpty()) {
                    int prev = navBackStack.pop();
                    isHandlingBack = true;
                    bottomNavigationView.setSelectedItemId(prev);
                    isHandlingBack = false;
                    return;
                }

                if (currentNavId != R.id.nav_home) {
                    isHandlingBack = true;
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    isHandlingBack = false;
                    return;
                }

                // Đang ở Home, không còn trang trước → để hệ thống thoát app
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void initViews() {
        rvFeaturedSales      = findViewById(R.id.rv_featured_sales);
        rvRentalCars         = findViewById(R.id.rv_new_rentals);
        rvBanners            = findViewById(R.id.rv_banners);
        etSearch             = findViewById(R.id.et_search);
        tvGreeting           = findViewById(R.id.tv_greeting);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        // BottomNavigationView tự cộng inset thanh điều hướng hệ thống vào padding đáy,
        // làm icon lệch lên trên; thanh này dạng nổi nên không cần né inset
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> insets);
        // Né thanh điều hướng: cộng chiều cao thanh hệ thống vào margin đáy của khung menu
        View navContainer = findViewById(R.id.bottom_nav_container);
        int baseNavMargin = ((ViewGroup.MarginLayoutParams) navContainer.getLayoutParams()).bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(navContainer, (v, insets) -> {
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (lp.bottomMargin != baseNavMargin + navBarHeight) {
                lp.bottomMargin = baseNavMargin + navBarHeight;
                v.setLayoutParams(lp);
            }
            return insets;
        });
        fragmentContainer    = findViewById(R.id.fragment_container);
        homeLayout           = findViewById(R.id.home_layout);
        swipeRefreshLayout   = findViewById(R.id.swipe_refresh);
        nestedScroll         = findViewById(R.id.nested_scroll);
        headerLayout         = findViewById(R.id.header_layout);
        stickyBar            = findViewById(R.id.sticky_bar);

        // Quick action buttons ở header gốc
        btnBuyCar    = findViewById(R.id.btn_buy_car);
        btnDriver    = findViewById(R.id.btn_driver);
        btnPoliceCar = findViewById(R.id.btn_police_car);
        btnPayment   = findViewById(R.id.btn_payment);

        // Quick action buttons ở thanh xanh sau khi vuốt lên
        stickyBtnBuyCar    = findViewById(R.id.sticky_btn_buy_car);
        stickyBtnDriver    = findViewById(R.id.sticky_btn_driver);
        stickyBtnPoliceCar = findViewById(R.id.sticky_btn_police_car);
        stickyBtnPayment   = findViewById(R.id.sticky_btn_payment);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Khi vuốt lên:
     * - avatar + tên + thanh tìm kiếm biến mất khỏi màn hình
     * - 4 nút chức năng hiện trong khung xanh phía trên
     */
    private void setupScrollBehavior() {
        if (nestedScroll == null) return;

        // Thu gọn header bám theo độ cuộn (1:1 với ngón tay) để hiệu ứng thật mượt,
        // thay cho kiểu bật/tắt theo ngưỡng gây giật.
        nestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> applyHeaderCollapse(scrollY));
    }

    /**
     * Nội suy liên tục theo scrollY:
     * - header (avatar + tên + ô tìm kiếm) trượt lên và mờ dần
     * - thanh xanh 4 nút (sticky) trượt xuống và hiện dần
     * Hai phần crossfade lệch pha nhẹ để không bị chồng mờ ở giữa.
     */
    private void applyHeaderCollapse(int scrollY) {
        if (headerLayout == null || stickyBar == null) return;

        int headerMove = headerLayout.getHeight();
        if (headerMove <= 0) headerMove = dp(210);

        float progress = clamp01(scrollY / (float) headerMove);
        isStickyVisible = progress > 0f;

        // Header: trượt lên đúng theo ngón tay, mờ nhanh hơn (biến mất khi ~62%)
        float headerAlpha = clamp01(1f - progress * 1.6f);
        headerLayout.setTranslationY(-progress * headerMove);
        headerLayout.setAlpha(headerAlpha);
        headerLayout.setVisibility(headerAlpha <= 0f ? View.INVISIBLE : View.VISIBLE);

        // Sticky: bắt đầu hiện từ ~35% trở đi
        int stickyHeight = stickyBar.getHeight();
        if (stickyHeight <= 0) stickyHeight = dp(120);
        if (progress <= 0f) {
            stickyBar.setVisibility(View.GONE);
            stickyBar.setTranslationY(0f);
        } else {
            stickyBar.setVisibility(View.VISIBLE);
            stickyBar.setTranslationY(-(1f - progress) * stickyHeight);
            stickyBar.setAlpha(clamp01((progress - 0.35f) / 0.65f));
        }
    }

    private float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private void setupQuickActions() {
        View.OnClickListener openCategory = v -> {
            pulseView(v);
            bottomNavigationView.setSelectedItemId(R.id.nav_cataloge);
        };

        if (btnBuyCar != null) btnBuyCar.setOnClickListener(openCategory);
        if (btnDriver != null) btnDriver.setOnClickListener(openCategory);
        if (btnPoliceCar != null) btnPoliceCar.setOnClickListener(openCategory);
        if (btnPayment != null) btnPayment.setOnClickListener(openCategory);

        if (stickyBtnBuyCar != null) stickyBtnBuyCar.setOnClickListener(openCategory);
        if (stickyBtnDriver != null) stickyBtnDriver.setOnClickListener(openCategory);
        if (stickyBtnPoliceCar != null) stickyBtnPoliceCar.setOnClickListener(openCategory);
        if (stickyBtnPayment != null) stickyBtnPayment.setOnClickListener(openCategory);
    }

    private void pulseView(View view) {
        view.animate().cancel();
        view.animate()
                .scaleX(0.94f)
                .scaleY(0.94f)
                .setDuration(70)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();
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
                        if ("sold".equals(status) || "hidden".equals(status)) continue;

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
            // Ghi nhận tab đang đứng vào lịch sử để Back có thể quay lại (bỏ qua khi đang xử lý Back)
            if (!isHandlingBack && itemId != currentNavId) {
                navBackStack.push(currentNavId);
            }
            int prevNavId = currentNavId;
            currentNavId = itemId;
            // Hướng trượt: sang tab bên phải (+1) thì trang mới vào từ phải, bên trái (-1) thì vào từ trái
            int dir = Integer.signum(navIndex(itemId) - navIndex(prevNavId));

            if (itemId == R.id.nav_home) {
                goHome(prevNavId, dir);
                return true;
            } else if (itemId == R.id.nav_cataloge
                    || itemId == R.id.nav_manage
                    || itemId == R.id.nav_messages
                    || itemId == R.id.nav_account) {
                showFragment(itemId, prevNavId, dir);
                return true;
            }
            return false;
        });
    }

    /** Vị trí của tab trên thanh menu, dùng để xác định chiều trượt trái/phải. */
    private int navIndex(int itemId) {
        if (itemId == R.id.nav_home)     return 0;
        if (itemId == R.id.nav_cataloge) return 1;
        if (itemId == R.id.nav_manage)   return 2;
        if (itemId == R.id.nav_messages) return 3;
        if (itemId == R.id.nav_account)  return 4;
        return 0;
    }

    /** Quay về Home: trượt màn hình Home vào, đẩy fragment đang mở trượt ra cùng chiều. */
    private void goHome(int prevNavId, int dir) {
        homeLayout.setVisibility(View.VISIBLE);
        if (prevNavId != R.id.nav_home) {
            slideViewIn(homeLayout, dir);
            hideActiveFragment(dir);
        }
        resetHomeHeaderState();
        // Reset scroll về đầu khi về Home
        if (nestedScroll != null) nestedScroll.smoothScrollTo(0, 0);
    }

    /**
     * Hiện tab dạng fragment với hiệu ứng trượt ngang theo chiều di chuyển.
     * Fragment được giữ lại trong cache: lần đầu mới tạo & add, các lần sau chỉ show/hide
     * nên không phải dựng layout hay tải lại dữ liệu → chuyển tab mượt, không giật.
     */
    private void showFragment(int itemId, int prevNavId, int dir) {
        fragmentContainer.setVisibility(View.VISIBLE);
        // Từ Home chuyển sang tab: trượt Home ra rồi ẩn đi
        if (prevNavId == R.id.nav_home) {
            slideViewOut(homeLayout, dir);
        }

        int enter = dir >= 0 ? R.anim.nav_slide_in_right : R.anim.nav_slide_in_left;
        int exit  = dir >= 0 ? R.anim.nav_slide_out_left : R.anim.nav_slide_out_right;

        Fragment target = fragmentCache.get(itemId);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(enter, exit);

        if (target == null) {
            target = createFragment(itemId);
            fragmentCache.put(itemId, target);
            ft.add(R.id.fragment_container, target);
        } else {
            ft.show(target);
        }

        if (activeFragment != null && activeFragment != target) {
            ft.hide(activeFragment);
        }
        activeFragment = target;
        ft.commit();
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.nav_cataloge) return new CategoryFragment();
        if (itemId == R.id.nav_manage)   return new ManageFragment();
        if (itemId == R.id.nav_messages) return new MessagesFragment();
        return new ProfileFragment();
    }

    /** Ẩn fragment đang hiện với hiệu ứng trượt ra (khi về Home). */
    private void hideActiveFragment(int dir) {
        if (activeFragment == null) return;
        int enter = dir >= 0 ? R.anim.nav_slide_in_right : R.anim.nav_slide_in_left;
        int exit  = dir >= 0 ? R.anim.nav_slide_out_left : R.anim.nav_slide_out_right;
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(enter, exit)
                .hide(activeFragment)
                .commit();
        activeFragment = null;
    }

    private static final int SLIDE_DURATION_MS = 280;

    /** Trượt một View vào màn hình: dir>0 vào từ phải, dir<0 vào từ trái. */
    private void slideViewIn(View v, int dir) {
        int w = v.getWidth();
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        v.animate().cancel();
        v.setTranslationX(dir * w);
        v.setAlpha(0.6f);
        v.animate()
                .translationX(0f).alpha(1f)
                .setDuration(SLIDE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /** Trượt một View ra khỏi màn hình rồi ẩn: dir>0 ra bên trái, dir<0 ra bên phải. */
    private void slideViewOut(View v, int dir) {
        int w = v.getWidth();
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        v.animate().cancel();
        v.animate()
                .translationX(-dir * w).alpha(0.6f)
                .setDuration(SLIDE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    v.setVisibility(View.GONE);
                    v.setTranslationX(0f);
                    v.setAlpha(1f);
                })
                .start();
    }

    private void resetHomeHeaderState() {
        isStickyVisible = false;
        if (stickyBar != null) {
            stickyBar.animate().cancel();
            stickyBar.setVisibility(View.GONE);
            stickyBar.setTranslationY(0f);
            stickyBar.setAlpha(1f);
        }
        if (headerLayout != null) {
            headerLayout.animate().cancel();
            headerLayout.setVisibility(View.VISIBLE);
            headerLayout.setTranslationY(0f);
            headerLayout.setAlpha(1f);
        }
    }
}