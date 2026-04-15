package com.example.doanmb;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvFeaturedSales, rvRentalCars;
    private AutoCompleteTextView etSearch;

    // --- KHAI BÁO THÊM CHO BOTTOM NAVIGATION ---
    private BottomNavigationView bottomNavigationView;
    private FrameLayout fragmentContainer;
    private View homeLayout;

    // Khai báo các Adapter
    private CategoryAdapter categoryAdapter;
    private CarSaleAdapter carSaleAdapter;
    private CarRentalAdapter carRentalAdapter;

    private List<Car> saleCarList;
    private List<Car> rentalCarList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();
        loadDummyData();
        setupSearch();
        setupAutoComplete();
        setupBottomNavigation(); // Khởi chạy thanh điều hướng
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rv_categories);
        rvFeaturedSales = findViewById(R.id.rv_featured_sales);
        rvRentalCars = findViewById(R.id.rv_new_rentals);
        etSearch = findViewById(R.id.et_search);

        // Ánh xạ các layout mới
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);
        homeLayout = findViewById(R.id.home_layout);
    }

    // --- XỬ LÝ CHUYỂN TAB ĐÃ CẬP NHẬT ĐẦY ĐỦ 5 NÚT ---
    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Bấm nút Home: Hiện trang chủ
                if (homeLayout != null) homeLayout.setVisibility(View.VISIBLE);
                if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
                return true;

            } else if (itemId == R.id.nav_manage) {
                // Bấm nút Quản lý: Hiện ManageFragment
                if (homeLayout != null) homeLayout.setVisibility(View.GONE);
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ManageFragment()).commit();
                }
                return true;

            } else if (itemId == R.id.nav_post) {
                // Bấm nút Đăng tin: Mở trang Activity mới
                Intent intent = new Intent(MainActivity.this, PostCarActivity.class);
                startActivity(intent);
                return false; // Trả về false để không làm đổi màu icon đang chọn ở dưới đáy

            } else if (itemId == R.id.nav_messages) {
                // Bấm nút Tin nhắn: Hiện MessagesFragment
                if (homeLayout != null) homeLayout.setVisibility(View.GONE);
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MessagesFragment()).commit();
                }
                return true;

            } else if (itemId == R.id.nav_account) {
                // Bấm nút Cá nhân: Hiện ProfileFragment
                if (homeLayout != null) homeLayout.setVisibility(View.GONE);
                if (fragmentContainer != null) {
                    fragmentContainer.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
                }
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerViews() {
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedSales.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRentalCars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadDummyData() {
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category("Mua xe", android.R.drawable.ic_menu_directions));
        categoryList.add(new Category("Bán xe", android.R.drawable.ic_menu_send));
        categoryList.add(new Category("Thuê tự lái", android.R.drawable.ic_menu_compass));
        categoryList.add(new Category("Có tài xế", android.R.drawable.ic_menu_myplaces));

        categoryAdapter = new CategoryAdapter(categoryList);
        rvCategories.setAdapter(categoryAdapter);

        saleCarList = new ArrayList<>();
        saleCarList.add(new Car("Toyota Vios 2020", "450.000.000 VNĐ", "TP.HCM • 2020 • Tự động", android.R.drawable.ic_menu_gallery));
        saleCarList.add(new Car("Kia Morning 2021", "320.000.000 VNĐ", "Hà Nội • 2021 • Số sàn", android.R.drawable.ic_menu_gallery));
        saleCarList.add(new Car("Mazda 3 2019", "550.000.000 VNĐ", "Đà Nẵng • 2019 • Tự động", android.R.drawable.ic_menu_gallery));

        carSaleAdapter = new CarSaleAdapter(saleCarList, car -> {
            Intent intent = new Intent(MainActivity.this, CarDetailActivity.class);
            intent.putExtra("CAR_DATA", car);
            startActivity(intent);
        });
        rvFeaturedSales.setAdapter(carSaleAdapter);

        rentalCarList = new ArrayList<>();
        rentalCarList.add(new Car("Hyundai Accent 2022", "800.000đ / ngày", "Quận 1, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery));
        rentalCarList.add(new Car("Ford Ranger 2021", "1.200.000đ / ngày", "Dĩ An, Bình Dương • Tự động", android.R.drawable.ic_menu_gallery));
        rentalCarList.add(new Car("Honda City 2023", "900.000đ / ngày", "Thủ Đức, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery));

        carRentalAdapter = new CarRentalAdapter(rentalCarList);
        rvRentalCars.setAdapter(carRentalAdapter);
    }

    private void setupAutoComplete() {
        List<String> carNames = new ArrayList<>();
        for (Car car : saleCarList) carNames.add(car.getName());
        for (Car car : rentalCarList) carNames.add(car.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, carNames);
        etSearch.setAdapter(adapter);

        etSearch.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCarName = (String) parent.getItemAtPosition(position);
            filter(selectedCarName);
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String text) {
        List<Car> filteredSaleList = new ArrayList<>();
        for (Car car : saleCarList) {
            if (car.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredSaleList.add(car);
            }
        }
        carSaleAdapter.filterList(filteredSaleList);

        List<Car> filteredRentalList = new ArrayList<>();
        for (Car car : rentalCarList) {
            if (car.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredRentalList.add(car);
            }
        }
        carRentalAdapter.filterList(filteredRentalList);
    }
}