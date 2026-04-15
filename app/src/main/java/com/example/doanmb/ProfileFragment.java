package com.example.doanmb;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn;
    private ScrollView layoutLoggedIn;

    // KHAI BÁO THÊM 2 NÚT BẤM
    private Button btnLogin, btnRegister;

    // Biến kiểm tra trạng thái đăng nhập
    private boolean isLoggedIn = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);

        // ÁNH XẠ 2 NÚT BẤM
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);

        // BẮT SỰ KIỆN CLICK CHO NÚT ĐĂNG NHẬP
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        // BẮT SỰ KIỆN CLICK CHO NÚT ĐĂNG KÝ
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });

        checkLoginStatus();

        return view;
    }

    private void checkLoginStatus() {
        if (isLoggedIn) {
            layoutNotLoggedIn.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
        } else {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }
}