package com.example.doanmb;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvCategories, rvFeaturedSales, rvRentalCars;
    // Khai báo các Adapter ở đây (ví dụ: CategoryAdapter, CarAdapter)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerViews();
        loadDummyData();
    }

    private void initViews() {
        rvCategories = findViewById(R.id.rvCategories);
        rvFeaturedSales = findViewById(R.id.rvFeaturedSales);
        rvRentalCars = findViewById(R.id.rvRentalCars);
    }

    private void setupRecyclerViews() {
        // Cài đặt RecyclerView danh mục (cuộn ngang)
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Cài đặt RecyclerView xe bán nổi bật (cuộn ngang)
        rvFeaturedSales.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Cài đặt RecyclerView xe cho thuê (dạng lưới 2 cột cuộn dọc)
        // rvRentalCars.setLayoutManager(new GridLayoutManager(this, 2));
        rvRentalCars.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
    }

    private void loadDummyData() {
        // TODO: Khởi tạo danh sách đối tượng (List<Car>) và gắn vào Adapter
        // Ví dụ:
        // List<Car> saleCars = new ArrayList<>();
        // saleCars.add(new Car("Toyota Vios 2020", "450.000.000 VND", ...));
        // CarAdapter saleAdapter = new CarAdapter(saleCars);
        // rvFeaturedSales.setAdapter(saleAdapter);
    }
}