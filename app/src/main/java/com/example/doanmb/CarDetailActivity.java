package com.example.doanmb;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CarDetailActivity extends AppCompatActivity {

    private ImageView ivCarDetail;
    private TextView tvCarName, tvCarPrice, tvCarInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_detail);

        // Ánh xạ
        ivCarDetail = findViewById(R.id.ivCarDetail);
        tvCarName = findViewById(R.id.tvCarNameDetail);
        tvCarPrice = findViewById(R.id.tvCarPriceDetail);
        tvCarInfo = findViewById(R.id.tvCarInfoDetail);

        // Nhận dữ liệu từ MainActivity gửi qua
        Car car = (Car) getIntent().getSerializableExtra("CAR_DATA");

        if (car != null) {
            ivCarDetail.setImageResource(car.getImageResId());
            tvCarName.setText(car.getName());
            tvCarPrice.setText(car.getPrice());
            tvCarInfo.setText(car.getInfo());
        }

        // Thêm nút back trên Toolbar (tùy chọn)
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chi tiết xe");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // Đóng trang hiện tại và quay về
        return true;
    }
}