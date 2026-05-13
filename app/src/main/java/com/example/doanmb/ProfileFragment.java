package com.example.doanmb;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn;
    private LinearLayout layoutLoggedIn;
    private Button btnLogin, btnRegister, btnLogout;
    private Button btnFilterAll, btnFilterSale, btnFilterRental;
    private TextView tvProfileName, tvProfilePhone, tvCarCount, tvEmptyCars;
    private RecyclerView rvMyCars;

    private ProfileCarAdapter adapter;
    private List<Car> allCars = new ArrayList<>();
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        layoutNotLoggedIn = view.findViewById(R.id.layout_not_logged_in);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        btnLogin = view.findViewById(R.id.btn_login);
        btnRegister = view.findViewById(R.id.btn_register);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfilePhone = view.findViewById(R.id.tv_profile_phone);
        tvCarCount = view.findViewById(R.id.tv_car_count);
        tvEmptyCars = view.findViewById(R.id.tv_empty_cars);
        rvMyCars = view.findViewById(R.id.rv_my_cars);
        btnFilterAll = view.findViewById(R.id.btn_filter_all);
        btnFilterSale = view.findViewById(R.id.btn_filter_sale);
        btnFilterRental = view.findViewById(R.id.btn_filter_rental);

        rvMyCars.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProfileCarAdapter(new ArrayList<>());
        rvMyCars.setAdapter(adapter);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegisterActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        btnFilterAll.setOnClickListener(v -> applyFilter("all"));
        btnFilterSale.setOnClickListener(v -> applyFilter("sale"));
        btnFilterRental.setOnClickListener(v -> applyFilter("rental"));

        return view;
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
        } else {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutLoggedIn.setVisibility(View.GONE);
        }
    }

    private void loadUserInfo(FirebaseUser user) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        if (tvProfileName != null) tvProfileName.setText(name != null ? name : "");
                        if (tvProfilePhone != null) tvProfilePhone.setText(phone != null ? phone : "");
                    }
                });
    }

    private void loadMyCars(String userId) {
        FirebaseFirestore.getInstance()
                .collection("cars")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    allCars.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        if (name == null) continue;
                        allCars.add(new Car(name, price != null ? price : "", info != null ? info : "", type != null ? type : "", android.R.drawable.ic_menu_gallery));
                    }
                    applyFilter(currentFilter);
                })
                .addOnFailureListener(e -> android.util.Log.e("ProfileFragment", "Lỗi load xe: " + e.getMessage()));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;

        // Cập nhật màu nút filter
        int active = Color.parseColor("#1976D2");
        int inactive = Color.parseColor("#90A4AE");
        btnFilterAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("all") ? active : inactive));
        btnFilterSale.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("sale") ? active : inactive));
        btnFilterRental.setBackgroundTintList(android.content.res.ColorStateList.valueOf(filter.equals("rental") ? active : inactive));

        List<Car> filtered = new ArrayList<>();
        for (Car car : allCars) {
            String type = car.getType() != null ? car.getType().toLowerCase() : "";
            boolean isSale = type.equals("sale") || type.contains("bán") || type.contains("ban");
            boolean isRental = type.equals("rental") || type.contains("thuê") || type.contains("thue");
            if (filter.equals("all") || (filter.equals("sale") && isSale) || (filter.equals("rental") && isRental)) {
                filtered.add(car);
            }
        }

        adapter.updateList(filtered);
        tvCarCount.setText("Tin đã đăng: " + filtered.size());
        tvEmptyCars.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvMyCars.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
