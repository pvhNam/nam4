package com.example.doanmb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private Button btnDoLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Setup nút Back trên thanh tiêu đề
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Đăng nhập");
        }

        // 2. Ánh xạ View
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnDoLogin = findViewById(R.id.btn_do_login);
        tvGoToRegister = findViewById(R.id.tv_go_to_register);

        // 3. Xử lý khi bấm nút Đăng Nhập
        btnDoLogin.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(phone.isEmpty() || password.isEmpty()){
                Toast.makeText(LoginActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: Sau này móc API thật vào đây.
                // Hiện tại giả vờ đăng nhập thành công:
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                // Trở về trang trước đó
                finish();
            }
        });

        // 4. Xử lý khi bấm "Đăng ký ngay"
        tvGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish(); // Đóng trang Login để tiết kiệm bộ nhớ
        });
    }

    // Bắt sự kiện bấm nút Back trên góc trái
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}