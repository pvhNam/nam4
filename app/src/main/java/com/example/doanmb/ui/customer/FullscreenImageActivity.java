package com.example.doanmb.ui.customer;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.doanmb.databinding.ActivityFullscreenImageBinding;

/** Xem ảnh toàn màn hình (pinch-to-zoom). Màn thuần hiển thị nên không cần ViewModel. */
public class FullscreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "IMAGE_URL";

    private ActivityFullscreenImageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String originalUrl = imageUrl.contains("cloudinary.com")
                    ? imageUrl.replaceAll("/upload/[^/]+/", "/upload/")
                    : imageUrl;
            Glide.with(this)
                    .load(originalUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.photoView);
        }

        binding.btnCloseFullscreen.setOnClickListener(v -> finish());
        binding.photoView.setOnViewTapListener((view, x, y) -> { /* chạm 1 lần: tuỳ chọn đóng */ });
    }
}
