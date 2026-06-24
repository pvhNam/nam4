package com.example.doanmb.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.adapter.ProfileCarAdapter;
import com.example.doanmb.databinding.ActivityFavoriteCarsBinding;
import com.example.doanmb.model.Car;

import java.util.ArrayList;
import java.util.List;

/** Danh sách xe yêu thích — MVVM. */
public class FavoriteCarsActivity extends AppCompatActivity {

    private ActivityFavoriteCarsBinding binding;
    private FavoriteViewModel viewModel;
    private ProfileCarAdapter adapter;
    private final List<Car> favCars = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteCarsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FavoriteViewModel.class);

        binding.btnBackFavorites.setOnClickListener(v -> finish());
        binding.rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProfileCarAdapter(favCars, this::openCarDetail);
        binding.rvFavorites.setAdapter(adapter);

        viewModel.getFavCars().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void render(List<Car> cars) {
        favCars.clear();
        if (cars != null) favCars.addAll(cars);
        adapter.updateList(favCars);
        boolean empty = favCars.isEmpty();
        binding.tvFavoritesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvFavorites.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openCarDetail(Car car) {
        Intent intent = new Intent(this, CarDetailActivity.class);
        intent.putExtra("CAR_DATA", car);
        intent.putExtra("CAR_ID", car.getId());
        intent.putExtra("SELLER_ID", car.getSellerId());
        intent.putExtra("CAR_TYPE", car.getType());
        startActivity(intent);
    }
}
