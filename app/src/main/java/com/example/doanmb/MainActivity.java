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

import com.example.doanmb.ui.activity.CarDetailActivity;
import com.example.doanmb.ui.fragment.CategoryFragment;
import com.example.doanmb.ui.fragment.ManageFragment;
import com.example.doanmb.ui.fragment.MessagesFragment;
import com.example.doanmb.ui.fragment.ProfileFragment;
import com.example.doanmb.adapter.CarRentalAdapter;
import com.example.doanmb.adapter.CarSaleAdapter;
import com.example.doanmb.adapter.CategoryAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.model.Category;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import com.cloudinary.android.MediaManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Luôn đăng xuất khi khởi động app → bắt đầu từ trạng thái chưa đăng nhập
        FirebaseAuth.getInstance().signOut();

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        loadCategories();
        loadCarsFromFirestore();
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
                    saleCarList.clear();
                    rentalCarList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        String sellerId = doc.getString("sellerId");
                        String status = doc.getString("status");
                        String imageUrl = doc.getString("imageUrl");

                        if (name == null) continue;
                        // Ẩn xe đã bán
                        if ("sold".equals(status)) continue;

                        Car car = new Car(name, price != null ? price : "", info != null ? info : "", android.R.drawable.ic_menu_gallery);
                        car.setId(doc.getId());
                        car.setType(type != null ? type : "");
                        car.setImageUrl(imageUrl != null ? imageUrl : "");
                        car.setSellerId(sellerId != null ? sellerId : "");

                        // Badge đang có người đặt — giữ đủ tất cả thông tin
                        if ("holding".equals(status)) {
                            Car holdingCar = new Car("⏳ " + name, price != null ? price : "", "Đang có người đặt • " + (info != null ? info : ""), android.R.drawable.ic_menu_gallery);
                            holdingCar.setId(doc.getId());
                            holdingCar.setType(type != null ? type : "");
                            holdingCar.setImageUrl(imageUrl != null ? imageUrl : "");
                            holdingCar.setSellerId(sellerId != null ? sellerId : "");
                            car = holdingCar;
                        }

                        String normalizedType = type != null ? type.toLowerCase().trim() : "";
                        if (normalizedType.equals("sale")) {
                            saleCarList.add(car);
                        } else if (normalizedType.equals("rental")) {
                            rentalCarList.add(car);
                        }
                    }

                    carSaleAdapter.filterList(saleCarList);
                    carRentalAdapter.filterList(rentalCarList);
                    setupAutoComplete();
                })
                .addOnFailureListener(e -> loadDummyData());
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
            public void afterTextChanged(Editable s) { filter(s.toString()); }
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
            } else if (itemId == R.id.nav_cataloge) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new CategoryFragment()).commit();
                return true;
            } else if (itemId == R.id.nav_manage) {
                homeLayout.setVisibility(View.GONE);
                fragmentContainer.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ManageFragment()).commit();
                return true;
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