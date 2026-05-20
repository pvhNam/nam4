package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.OrderHistoryAdapter;
import com.example.doanmb.adapter.ProfileCarAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.activity.LoginActivity;
import com.example.doanmb.ui.activity.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn, layoutLoggedIn;
    private Button btnTabMyCars, btnTabMyOrders;
    private LinearLayout layoutTabMyCars, layoutTabMyOrders;
    private Button btnLogin, btnRegister, btnLogout;
    private TextView tvProfileName, tvProfilePhone;

    // Tab 1: Xe đã đăng
    private Button btnFilterAll, btnFilterSale, btnFilterRental;
    private TextView tvCarCount, tvEmptyCars;
    private RecyclerView rvMyCars;
    private ProfileCarAdapter carAdapter;
    private List<Car> allCars = new ArrayList<>();
    private String currentFilter = "all";

    // Tab 2: Lịch sử đơn
    private TextView tvOrderCount, tvEmptyOrders;
    private RecyclerView rvMyOrders;
    private OrderHistoryAdapter orderAdapter;
    private List<Map<String, Object>> orderList = new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfilePhone = view.findViewById(R.id.tv_profile_phone);
        btnTabMyCars = view.findViewById(R.id.btnTabMyCars);
        btnTabMyOrders = view.findViewById(R.id.btnTabMyOrders);
        layoutTabMyCars = view.findViewById(R.id.layoutTabMyCars);
        layoutTabMyOrders = view.findViewById(R.id.layoutTabMyOrders);

        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterSale = view.findViewById(R.id.btn_filter_sale);
        btnFilterRental = view.findViewById(R.id.btn_filter_rental);
        tvCarCount = view.findViewById(R.id.tv_car_count);
        tvEmptyCars = view.findViewById(R.id.tv_empty_cars);
        rvMyCars = view.findViewById(R.id.rv_my_cars);
        rvMyCars.setLayoutManager(new LinearLayoutManager(getContext()));
        carAdapter = new ProfileCarAdapter(new ArrayList<>());
        rvMyCars.setAdapter(carAdapter);

        tvOrderCount = view.findViewById(R.id.tvOrderCount);
        tvEmptyOrders = view.findViewById(R.id.tvEmptyOrders);
        rvMyOrders = view.findViewById(R.id.rvMyOrders);
        rvMyOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderHistoryAdapter(new ArrayList<>());
        rvMyOrders.setAdapter(orderAdapter);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });
        btnTabMyCars.setOnClickListener(v -> showTab(true));
        btnTabMyOrders.setOnClickListener(v -> showTab(false));
        btnFilterAll.setOnClickListener(v -> applyFilter("all"));
        btnFilterSale.setOnClickListener(v -> applyFilter("sale"));
        btnFilterRental.setOnClickListener(v -> applyFilter("rental"));
    }

    private void showTab(boolean showCars) {
        if (showCars) {
            layoutTabMyCars.setVisibility(View.VISIBLE);
            layoutTabMyOrders.setVisibility(View.GONE);
            btnTabMyCars.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabMyCars.setTextColor(0xFFFFFFFF);
            btnTabMyOrders.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabMyOrders.setTextColor(0xFF1976D2);
        } else {
            layoutTabMyCars.setVisibility(View.GONE);
            layoutTabMyOrders.setVisibility(View.VISIBLE);
            btnTabMyOrders.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabMyOrders.setTextColor(0xFFFFFFFF);
            btnTabMyCars.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabMyCars.setTextColor(0xFF1976D2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkLoginStatus();
    }

    private void checkLoginStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            layoutNotLoggedIn.setVisibility(View.GONE);
            layoutLoggedIn.setVisibility(View.VISIBLE);
            loadUserInfo(user);
            loadMyCars(user.getUid());
            loadMyOrders(user.getUid());
        } else {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void loadUserInfo(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        if (tvProfileName != null) tvProfileName.setText(name != null ? name : "");
                        if (tvProfilePhone != null) tvProfilePhone.setText(phone != null ? "📞 " + phone : "");
                    }
                });
    }

    private void loadMyCars(String userId) {
        db.collection("cars").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snapshots -> {
                    allCars.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        if (name == null) continue;
                        Car car = new Car(name, price != null ? price : "", info != null ? info : "", android.R.drawable.ic_menu_gallery);
                        car.setId(doc.getId());
                        car.setType(type != null ? type : "");
                        allCars.add(car);
                    }
                    applyFilter(currentFilter);
                });
    }

    private void loadMyOrders(String userId) {
        db.collection("orders").whereEqualTo("buyerId", userId).get()
                .addOnSuccessListener(snapshots -> {
                    orderList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        orderList.add(doc.getData());
                    }
                    orderAdapter.updateList(orderList);
                    tvOrderCount.setText("Đơn của tôi: " + orderList.size());
                    tvEmptyOrders.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvMyOrders.setVisibility(orderList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        int active = Color.parseColor("#1976D2");
        int inactive = Color.parseColor("#90A4AE");
        btnFilterAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("all") ? active : inactive));
        btnFilterSale.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("sale") ? active : inactive));
        btnFilterRental.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("rental") ? active : inactive));

        List<Car> filtered = new ArrayList<>();
        for (Car car : allCars) {
            String type = car.getType() != null ? car.getType().toLowerCase() : "";
            if (filter.equals("all") || filter.equals(type)) {
                filtered.add(car);
            }
        }

        carAdapter.updateList(filtered);
        tvCarCount.setText("Tin đã đăng: " + filtered.size());
        tvEmptyCars.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvMyCars.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }
}