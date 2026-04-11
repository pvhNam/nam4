package com.example.doanmb;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvFeaturedSales, rvRentalCars;

    // Khai báo các Adapter
    private CategoryAdapter categoryAdapter;
    private CarSaleAdapter carSaleAdapter;
    private CarRentalAdapter carRentalAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();
        loadDummyData();
    }

    private void initViews() {
        // Đã cập nhật lại ID cho khớp chính xác với file activity_main.xml
        rvCategories = findViewById(R.id.rv_categories);
        rvFeaturedSales = findViewById(R.id.rv_featured_sales);
        rvRentalCars = findViewById(R.id.rv_new_rentals);
    }

    private void setupRecyclerViews() {
        // Cài đặt RecyclerView danh mục (cuộn ngang)
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Cài đặt RecyclerView xe bán nổi bật (cuộn ngang)
        rvFeaturedSales.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Cài đặt RecyclerView xe cho thuê (cuộn ngang)
        rvRentalCars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadDummyData() {
        // 1. Tạo dữ liệu cho Danh mục dịch vụ
        List<Category> categoryList = new ArrayList<>();
        // Sử dụng tạm icon mặc định của Android để test
        categoryList.add(new Category("Mua xe", android.R.drawable.ic_menu_directions));
        categoryList.add(new Category("Bán xe", android.R.drawable.ic_menu_send));
        categoryList.add(new Category("Thuê tự lái", android.R.drawable.ic_menu_compass));
        categoryList.add(new Category("Có tài xế", android.R.drawable.ic_menu_myplaces));

        categoryAdapter = new CategoryAdapter(categoryList);
        rvCategories.setAdapter(categoryAdapter);

        // 2. Tạo dữ liệu cho Xe đang bán nổi bật
        List<Car> saleCarList = new ArrayList<>();
        saleCarList.add(new Car("Toyota Vios 2020", "450.000.000 VNĐ", "TP.HCM • 2020 • Tự động", android.R.drawable.ic_menu_gallery));
        saleCarList.add(new Car("Kia Morning 2021", "320.000.000 VNĐ", "Hà Nội • 2021 • Số sàn", android.R.drawable.ic_menu_gallery));
        saleCarList.add(new Car("Mazda 3 2019", "550.000.000 VNĐ", "Đà Nẵng • 2019 • Tự động", android.R.drawable.ic_menu_gallery));

        carSaleAdapter = new CarSaleAdapter(saleCarList);
        rvFeaturedSales.setAdapter(carSaleAdapter);

        // 3. Tạo dữ liệu cho Xe cho thuê mới nhất
        List<Car> rentalCarList = new ArrayList<>();
        rentalCarList.add(new Car("Hyundai Accent 2022", "800.000đ / ngày", "Quận 1, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery));
        rentalCarList.add(new Car("Ford Ranger 2021", "1.200.000đ / ngày", "Dĩ An, Bình Dương • Tự động", android.R.drawable.ic_menu_gallery));
        rentalCarList.add(new Car("Honda City 2023", "900.000đ / ngày", "Thủ Đức, TP.HCM • Tự động", android.R.drawable.ic_menu_gallery));

        carRentalAdapter = new CarRentalAdapter(rentalCarList);
        rvRentalCars.setAdapter(carRentalAdapter);
    }
}