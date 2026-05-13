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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvFeaturedSales, rvRentalCars;
    private AutoCompleteTextView etSearch;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout fragmentContainer;
    private View homeLayout;

    private CategoryAdapter categoryAdapter;
    private CarSaleAdapter carSaleAdapter;
    private CarRentalAdapter carRentalAdapter;

    private List<Car> saleCarList = new ArrayList<>();
    private List<Car> rentalCarList = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        loadCategories();
        loadCarsFromFirestore(); // Load từ Firebase
        setupSearch();
        setupBottomNavigation();
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rv_categories);
        rvFeaturedSales = findViewById(R.id.rv_featured_sales);
        rvRentalCars = findViewById(R.id.rv_new_rentals);
        etSearch = findViewById(R.id.et_search);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);
        homeLayout = findViewById(R.id.home_layout);
    }

    private void setupRecyclerViews() {
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedSales.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRentalCars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Khởi tạo adapter rỗng trước
        carSaleAdapter = new CarSaleAdapter(saleCarList, car -> {
            Intent intent = new Intent(MainActivity.this, CarDetailActivity.class);
            intent.putExtra("CAR_DATA", car);
            startActivity(intent);
        });
        rvFeaturedSales.setAdapter(carSaleAdapter);

        carRentalAdapter = new CarRentalAdapter(rentalCarList);
        rvRentalCars.setAdapter(carRentalAdapter);
    }

    private void loadCategories() {
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category("Mua xe", android.R.drawable.ic_menu_directions));
        categoryList.add(new Category("Bán xe", android.R.drawable.ic_menu_send));
        categoryList.add(new Category("Thuê tự lái", android.R.drawable.ic_menu_compass));
        categoryList.add(new Category("Có tài xế", android.R.drawable.ic_menu_myplaces));
        categoryAdapter = new CategoryAdapter(categoryList);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void loadCarsFromFirestore() {
        db.collection("cars").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("DEBUG", "Tổng docs: " + queryDocumentSnapshots.size());

                    saleCarList.clear();
                    rentalCarList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");

                        android.util.Log.d("DEBUG", "Xe: " + name + " | type: " + type);

                        if (name == null) continue;
                        int imageResId = android.R.drawable.ic_menu_gallery;
                        String normalizedType = type != null ? type.toLowerCase().trim() : "";
                        Car car = new Car(name, price, info, normalizedType, imageResId);

                        if (normalizedType.equals("sale") || normalizedType.contains("bán") || normalizedType.contains("ban")) {
                            saleCarList.add(car);
                        } else if (normalizedType.equals("rental") || normalizedType.contains("thuê") || normalizedType.contains("thue")) {
                            rentalCarList.add(car);
                        }
                    }

                    carSaleAdapter.filterList(saleCarList);
                    carRentalAdapter.filterList(rentalCarList);
                    setupAutoComplete();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DEBUG", "LỖI: " + e.getMessage());
                    loadDummyData();
                });
    }

    private void loadDummyData() {
        saleCarList.add(new Car("Toyota Vios 2020", "450.000.000 VNĐ", "TP.HCM • 2020 • Tự động", android.R.drawable.ic_menu_gallery));
        saleCarList.add(new Car("Kia Morning 2021", "320.000.000 VNĐ", "Hà Nội • 2021 • Số sàn", android.R.drawable.ic_menu_gallery));
        rentalCarList.add(new Car("Hyundai Accent 2022", "800.000đ / ngày", "Quận 1, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery));
        carSaleAdapter.filterList(saleCarList);
        carRentalAdapter.filterList(rentalCarList);
    }

    private void setupAutoComplete() {
        List<String> carNames = new ArrayList<>();
        for (Car car : saleCarList) carNames.add(car.getName());
        for (Car car : rentalCarList) carNames.add(car.getName());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, carNames);
        etSearch.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });
    }

    private void filter(String text) {
        List<Car> filteredSale = new ArrayList<>();
        for (Car car : saleCarList) {
            if (car.getName().toLowerCase().contains(text.toLowerCase()))
                filteredSale.add(car);
        }
        carSaleAdapter.filterList(filteredSale);

        List<Car> filteredRental = new ArrayList<>();
        for (Car car : rentalCarList) {
            if (car.getName().toLowerCase().contains(text.toLowerCase()))
                filteredRental.add(car);
        }
        carRentalAdapter.filterList(filteredRental);
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                homeLayout.setVisibility(View.VISIBLE);
                fragmentContainer.setVisibility(View.GONE);
                return true;
            } else if (itemId == R.id.nav_manage) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ManageFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_post) {
                startActivity(new Intent(MainActivity.this, PostCarActivity.class));
                return false;
            } else if (itemId == R.id.nav_messages) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MessagesFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_account) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).commit();
                return true;
            }
            return false;
        });
    }
}