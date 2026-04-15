package com.example.doanmb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etPassword, etConfirm;
    private Button btnRegister;
    private TextView tvBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ View
        etName = findViewById(R.id.et_register_name);
        etPhone = findViewById(R.id.et_register_phone);
        etPassword = findViewById(R.id.et_register_password);
        etConfirm = findViewById(R.id.et_register_confirm_password);
        btnRegister = findViewById(R.id.btn_do_register);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        btnRegister.setOnClickListener(v -> {
            String password = etPassword.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (!password.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Quay lại trang đăng nhập
            }
        });

        tvBackToLogin.setOnClickListener(v -> finish());
    }
}