package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.adapter.ProfileCarAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.activity.CarDetailActivity;
import com.example.doanmb.ui.activity.PostCarActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryFragment extends Fragment {

    private static final String CATEGORY_SALE = "sale";
    private static final String CATEGORY_RENTAL = "rental";
    private static final String CATEGORY_DRIVER = "driver";
    private static final String BRAND_ALL = "all";
    private static final String[] KNOWN_BRANDS = {"Toyota", "Honda", "Mazda", "Kia", "Ford", "Hyundai", "VinFast", "Khác"};

    private CardView cardBuyCar;
    private CardView cardSellCar;
    private CardView cardSelfDrive;
    private CardView cardWithDriver;
    private LinearLayout tabBuyCarContent;
    private LinearLayout tabSelfDriveContent;
    private LinearLayout tabWithDriverContent;
    private TextView tvTabBuyCar;
    private TextView tvTabSelfDrive;
    private TextView tvTabWithDriver;
    private TextView tvResultTitle;
    private TextView tvCategoryCount;
    private TextView tvEmptyCategory;
    private LinearLayout layoutBrandFilters;
    private RecyclerView rvCategoryCars;

    private final List<Car> allCars = new ArrayList<>();
    private final List<String> brandFilters = new ArrayList<>();
    private ProfileCarAdapter carAdapter;
    private String currentCategory = CATEGORY_SALE;
    private String currentTitle = "Xe đang bán";
    private String selectedBrand = BRAND_ALL;
    private boolean shouldRefreshOnResume = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        cardBuyCar = view.findViewById(R.id.card_buy_car);
        cardSellCar = view.findViewById(R.id.card_sell_car);
        cardSelfDrive = view.findViewById(R.id.card_self_drive);
        cardWithDriver = view.findViewById(R.id.card_with_driver);
        tabBuyCarContent = view.findViewById(R.id.tab_buy_car_content);
        tabSelfDriveContent = view.findViewById(R.id.tab_self_drive_content);
        tabWithDriverContent = view.findViewById(R.id.tab_with_driver_content);
        tvTabBuyCar = view.findViewById(R.id.tv_tab_buy_car);
        tvTabSelfDrive = view.findViewById(R.id.tv_tab_self_drive);
        tvTabWithDriver = view.findViewById(R.id.tv_tab_with_driver);
        tvResultTitle = view.findViewById(R.id.tv_category_result_title);
        tvCategoryCount = view.findViewById(R.id.tv_category_count);
        tvEmptyCategory = view.findViewById(R.id.tv_empty_category);
        layoutBrandFilters = view.findViewById(R.id.layout_brand_filters);
        rvCategoryCars = view.findViewById(R.id.rv_category_cars);

        rvCategoryCars.setLayoutManager(new LinearLayoutManager(getContext()));
        carAdapter = new ProfileCarAdapter(new ArrayList<>(), car -> {
            Intent intent = new Intent(getActivity(), CarDetailActivity.class);
            intent.putExtra("CAR_DATA", car);
            intent.putExtra("CAR_ID", car.getId());
            intent.putExtra("SELLER_ID", car.getSellerId());
            intent.putExtra("CAR_TYPE", car.getType());
            startActivity(intent);
        });
        rvCategoryCars.setAdapter(carAdapter);

        setupCategoryActions();
        setupDefaultBrandFilters();
        loadCars();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shouldRefreshOnResume) {
            shouldRefreshOnResume = false;
            loadCars();
        }
    }

    private void setupCategoryActions() {
        cardBuyCar.setOnClickListener(v -> selectCategory(CATEGORY_SALE, "Xe đang bán"));
        cardSelfDrive.setOnClickListener(v -> selectCategory(CATEGORY_RENTAL, "Xe thuê tự lái"));
        cardWithDriver.setOnClickListener(v -> selectCategory(CATEGORY_DRIVER, "Xe có tài xế"));
        cardSellCar.setOnClickListener(v -> {
            shouldRefreshOnResume = true;
            startActivity(new Intent(getActivity(), PostCarActivity.class));
        });

        updateSelectedCategory();
    }

    private void selectCategory(String category, String title) {
        currentCategory = category;
        currentTitle = title;
        updateSelectedCategory();
        applyFilter();
    }

    private void updateSelectedCategory() {
        cardBuyCar.setCardBackgroundColor(Color.TRANSPARENT);
        cardSellCar.setCardBackgroundColor(Color.TRANSPARENT);
        cardSelfDrive.setCardBackgroundColor(Color.TRANSPARENT);
        cardWithDriver.setCardBackgroundColor(Color.TRANSPARENT);

        setTabSelected(tabBuyCarContent, tvTabBuyCar, currentCategory.equals(CATEGORY_SALE));
        setTabSelected(tabSelfDriveContent, tvTabSelfDrive, currentCategory.equals(CATEGORY_RENTAL));
        setTabSelected(tabWithDriverContent, tvTabWithDriver, currentCategory.equals(CATEGORY_DRIVER));
    }

    private void setTabSelected(LinearLayout tabContent, TextView tabLabel, boolean selected) {
        if (selected) {
            tabContent.setBackgroundResource(R.drawable.bg_tab_active_pill);
            tabLabel.setTextColor(Color.parseColor("#2F54D4"));
        } else {
            tabContent.setBackground(null);
            tabLabel.setTextColor(Color.WHITE);
        }
    }

    private void loadCars() {
        tvEmptyCategory.setText("Đang tải danh sách...");
        tvEmptyCategory.setVisibility(View.VISIBLE);
        rvCategoryCars.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("cars")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // PHÒNG HỘ: Nếu người dùng đã thoát Fragment, dừng xử lý ngay lập tức
                    if (!isAdded() || getActivity() == null) return;

                    allCars.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) continue;

                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        String brand = doc.getString("brand");
                        String imageUrl = doc.getString("imageUrl");
                        String sellerId = doc.getString("sellerId"); // ← thêm dòng này
                        Car car = new Car(
                                name,
                                price != null ? price : "",
                                info != null ? info : "",
                                type != null ? type : "",
                                resolveBrand(name, info, brand),
                                android.R.drawable.ic_menu_gallery
                        );
                        car.setId(doc.getId());
                        car.setImageUrl(imageUrl != null ? imageUrl : "");
                        car.setSellerId(sellerId != null ? sellerId : ""); // ← thêm dòng này
                        allCars.add(car);
                    }
                    updateBrandFilters();
                    applyFilter();
                })
                .addOnFailureListener(e -> {
                    // PHÒNG HỘ: Đảm bảo an toàn khi thất bại
                    if (!isAdded() || getActivity() == null) return;
                    allCars.clear();
                    loadSampleCars();
                    updateBrandFilters();
                    applyFilter();
                });
    }

    private void loadSampleCars() {
        allCars.add(new Car("Toyota Vios 2020", "450.000.000 VNĐ", "TP.HCM - 2020 - Tự động", "sale", "Toyota", android.R.drawable.ic_menu_gallery));
        allCars.add(new Car("Kia Morning 2021", "320.000.000 VNĐ", "Hà Nội - 2021 - Số sàn", "sale", "Kia", android.R.drawable.ic_menu_gallery));
        allCars.add(new Car("Hyundai Accent 2022", "800.000đ / ngày", "Quận 1, TP.HCM - Tự lái", "rental", "Hyundai", android.R.drawable.ic_menu_gallery));
        allCars.add(new Car("Toyota Fortuner 2023", "1.600.000đ / ngày", "TP.HCM - Có tài xế", "driver", "Toyota", android.R.drawable.ic_menu_gallery));
    }

    private void applyFilter() {
        List<Car> filteredCars = new ArrayList<>();

        for (Car car : allCars) {
            String type = normalizeText(car.getType());
            boolean matchesCategory =
                    (currentCategory.equals(CATEGORY_SALE) && isSaleType(type))
                            || (currentCategory.equals(CATEGORY_RENTAL) && isRentalType(type) && !isDriverType(type))
                            || (currentCategory.equals(CATEGORY_DRIVER) && isDriverType(type));

            if (matchesCategory && matchesSelectedBrand(car)) {
                filteredCars.add(car);
            }
        }

        carAdapter.updateList(filteredCars);
        tvResultTitle.setText(currentTitle);
        tvCategoryCount.setText(filteredCars.size() + " tin phù hợp");

        boolean isEmpty = filteredCars.isEmpty();
        rvCategoryCars.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmptyCategory.setText("Chưa có xe trong danh mục này");
        tvEmptyCategory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void setupDefaultBrandFilters() {
        brandFilters.clear();
        brandFilters.add(BRAND_ALL);
        for (String brand : KNOWN_BRANDS) {
            addBrandFilter(brand);
        }
        renderBrandFilters();
    }

    private void updateBrandFilters() {
        setupDefaultBrandFilters();
        for (Car car : allCars) {
            addBrandFilter(resolveBrand(car.getName(), car.getInfo(), car.getBrand()));
        }

        if (!brandFilters.contains(selectedBrand)) {
            selectedBrand = BRAND_ALL;
        }
        renderBrandFilters();
    }

    private void addBrandFilter(String brand) {
        if (brand == null || brand.trim().isEmpty()) return;

        for (String existingBrand : brandFilters) {
            if (normalizeText(existingBrand).equals(normalizeText(brand))) {
                return;
            }
        }
        brandFilters.add(brand.trim());
    }

    private void renderBrandFilters() {
        if (layoutBrandFilters == null) return;
        // PHÒNG HỘ TRỰC TIẾP LỖI LINE 238 LOGCAT:
        if (!isAdded() || getContext() == null) return;

        layoutBrandFilters.removeAllViews();
        for (String brand : brandFilters) {
            boolean selected = selectedBrand.equals(brand);
            TextView chip = new TextView(requireContext());
            chip.setText(brand.equals(BRAND_ALL) ? "Tất cả" : brand);
            chip.setTextColor(selected ? Color.WHITE : Color.parseColor("#333333"));
            chip.setTextSize(13);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setBackground(createChipBackground(selected));
            chip.setOnClickListener(v -> {
                selectedBrand = brand;
                renderBrandFilters();
                applyFilter();
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMarginEnd(dp(8));
            layoutBrandFilters.addView(chip, params);
        }
    }

    private GradientDrawable createChipBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dp(18));
        drawable.setColor(selected ? Color.parseColor("#1976D2") : Color.WHITE);
        drawable.setStroke(dp(1), selected ? Color.parseColor("#1976D2") : Color.parseColor("#DADCE0"));
        return drawable;
    }

    private boolean matchesSelectedBrand(Car car) {
        if (selectedBrand.equals(BRAND_ALL)) return true;
        String carBrand = resolveBrand(car.getName(), car.getInfo(), car.getBrand());
        return normalizeText(carBrand).equals(normalizeText(selectedBrand));
    }

    private String resolveBrand(String name, String info, String brand) {
        if (brand != null && !brand.trim().isEmpty()) {
            return brand.trim();
        }

        String source = normalizeText((name != null ? name : "") + " " + (info != null ? info : ""));
        for (String knownBrand : KNOWN_BRANDS) {
            if (!knownBrand.equals("Khác") && source.contains(normalizeText(knownBrand))) {
                return knownBrand;
            }
        }
        return "Khác";
    }

    private boolean isSaleType(String type) {
        return type.contains("sale") || type.contains("ban") || type.contains("mua");
    }

    private boolean isRentalType(String type) {
        return type.contains("rental") || type.contains("rent") || type.contains("thue") || type.contains("tu lai");
    }

    private boolean isDriverType(String type) {
        return type.contains("driver") || type.contains("tai xe") || type.contains("co tai xe");
    }

    private String normalizeText(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
