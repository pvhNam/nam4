package com.example.doanmb.ui.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryFragment extends Fragment implements PostCarFragment.OnPostCarSubmittedListener {

    private static final String CATEGORY_SALE = "sale";
    private static final String CATEGORY_SELL = "sell";
    private static final String CATEGORY_RENTAL = "rental";
    private static final String CATEGORY_DRIVER = "driver";
    private static final String BRAND_ALL = "all";
    private static final String[] KNOWN_BRANDS = {"Toyota", "Honda", "Mazda", "Kia", "Ford", "Hyundai", "VinFast", "Khác"};
    private static final String LOCATION_ALL = "Tất cả khu vực";
    private static final String[] KNOWN_LOCATIONS = {
            "TP. Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Bình Dương",
            "Đồng Nai", "Hải Phòng", "Cần Thơ", "Khánh Hòa"};

    private CardView cardBuyCar;
    private CardView cardSellCar;
    private CardView cardSelfDrive;
    private CardView cardWithDriver;
    private LinearLayout tabBuyCarContent;
    private LinearLayout tabSellCarContent;
    private LinearLayout tabSelfDriveContent;
    private LinearLayout tabWithDriverContent;
    private TextView tvTabBuyCar;
    private TextView tvTabSellCar;
    private TextView tvTabSelfDrive;
    private TextView tvTabWithDriver;
    private TextView tvResultTitle;
    private TextView tvCategoryCount;
    private TextView tvEmptyCategory;
    private LinearLayout layoutCategoryBrowseContent;
    private LinearLayout layoutBrandFilters;
    private FrameLayout postCarFragmentContainer;
    private RecyclerView rvCategoryCars;

    private final List<Car> allCars = new ArrayList<>();
    private final List<String> brandFilters = new ArrayList<>();
    private ProfileCarAdapter carAdapter;
    private TextView tvSearchLocation;
    private String currentCategory = CATEGORY_SALE;
    private String currentTitle = "Xe đang bán";
    private String selectedBrand = BRAND_ALL;
    private String selectedLocation = ""; // rỗng = tất cả khu vực

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        cardBuyCar = view.findViewById(R.id.card_buy_car);
        cardSellCar = view.findViewById(R.id.card_sell_car);
        cardSelfDrive = view.findViewById(R.id.card_self_drive);
        cardWithDriver = view.findViewById(R.id.card_with_driver);
        tabBuyCarContent = view.findViewById(R.id.tab_buy_car_content);
        tabSellCarContent = view.findViewById(R.id.tab_sell_car_content);
        tabSelfDriveContent = view.findViewById(R.id.tab_self_drive_content);
        tabWithDriverContent = view.findViewById(R.id.tab_with_driver_content);
        tvTabBuyCar = view.findViewById(R.id.tv_tab_buy_car);
        tvTabSellCar = view.findViewById(R.id.tv_tab_sell_car);
        tvTabSelfDrive = view.findViewById(R.id.tv_tab_self_drive);
        tvTabWithDriver = view.findViewById(R.id.tv_tab_with_driver);
        tvResultTitle = view.findViewById(R.id.tv_category_result_title);
        tvCategoryCount = view.findViewById(R.id.tv_category_count);
        tvEmptyCategory = view.findViewById(R.id.tv_empty_category);
        layoutCategoryBrowseContent = view.findViewById(R.id.layout_category_browse_content);
        layoutBrandFilters = view.findViewById(R.id.layout_brand_filters);
        postCarFragmentContainer = view.findViewById(R.id.post_car_fragment_container);
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

        TextView btnSearchCar = view.findViewById(R.id.btn_search_car);
        if (btnSearchCar != null) {
            btnSearchCar.setOnClickListener(v -> performSearch());
        }

        tvSearchLocation = view.findViewById(R.id.tv_search_location);
        if (tvSearchLocation != null) {
            tvSearchLocation.setText(LOCATION_ALL);
            tvSearchLocation.setOnClickListener(v -> showLocationPicker());
        }

        setupCategoryActions();
        setupDefaultBrandFilters();
        loadCars();

        return view;
    }

    private void setupCategoryActions() {
        cardBuyCar.setOnClickListener(v -> selectCategory(CATEGORY_SALE, "Xe đang bán"));
        cardSelfDrive.setOnClickListener(v -> selectCategory(CATEGORY_RENTAL, "Xe thuê tự lái"));
        cardWithDriver.setOnClickListener(v -> selectCategory(CATEGORY_DRIVER, "Xe có tài xế"));
        cardSellCar.setOnClickListener(v -> showPostCarForm());

        updateSelectedCategory();
    }

    private void selectCategory(String category, String title) {
        currentCategory = category;
        currentTitle = title;
        showBrowseContent();
        updateSelectedCategory();
        applyFilter();
    }

    private void showPostCarForm() {
        currentCategory = CATEGORY_SELL;
        updateSelectedCategory();

        layoutCategoryBrowseContent.setVisibility(View.GONE);
        postCarFragmentContainer.setVisibility(View.VISIBLE);

        if (getChildFragmentManager().findFragmentById(R.id.post_car_fragment_container) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.post_car_fragment_container, new PostCarFragment())
                    .commit();
        }
    }

    private void showBrowseContent() {
        layoutCategoryBrowseContent.setVisibility(View.VISIBLE);
        postCarFragmentContainer.setVisibility(View.GONE);
    }

    @Override
    public void onPostCarSubmitted() {
        loadCars();
    }

    private void updateSelectedCategory() {
        cardBuyCar.setCardBackgroundColor(Color.TRANSPARENT);
        cardSellCar.setCardBackgroundColor(Color.TRANSPARENT);
        cardSelfDrive.setCardBackgroundColor(Color.TRANSPARENT);
        cardWithDriver.setCardBackgroundColor(Color.TRANSPARENT);

        setTabSelected(tabBuyCarContent, tvTabBuyCar, currentCategory.equals(CATEGORY_SALE));
        setTabSelected(tabSellCarContent, tvTabSellCar, currentCategory.equals(CATEGORY_SELL));
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
                        if ("hidden".equals(doc.getString("status"))) continue; // chủ bài đã ẩn

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

    /** Nút "Tìm xe": áp dụng bộ lọc hiện tại (loại xe + hãng + khu vực) và cuộn về đầu kết quả. */
    private void performSearch() {
        showBrowseContent();
        applyFilter();
        if (rvCategoryCars != null) rvCategoryCars.scrollToPosition(0);
        if (getContext() != null && tvCategoryCount != null) {
            Toast.makeText(getContext(), tvCategoryCount.getText(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Mở hộp thoại chọn khu vực để tìm xe theo địa điểm. */
    private void showLocationPicker() {
        if (getContext() == null) return;

        final String[] items = new String[KNOWN_LOCATIONS.length + 1];
        items[0] = LOCATION_ALL;
        System.arraycopy(KNOWN_LOCATIONS, 0, items, 1, KNOWN_LOCATIONS.length);

        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn khu vực")
                .setItems(items, (dialog, which) -> {
                    selectedLocation = (which == 0) ? "" : items[which];
                    if (tvSearchLocation != null) {
                        tvSearchLocation.setText(which == 0 ? LOCATION_ALL : items[which]);
                    }
                    applyFilter();
                })
                .show();
    }

    private boolean matchesSelectedLocation(Car car) {
        if (selectedLocation == null || selectedLocation.isEmpty()) return true;
        String haystack = normalizeText((car.getInfo() != null ? car.getInfo() : "")
                + " " + (car.getName() != null ? car.getName() : ""));
        return haystack.contains(normalizeText(selectedLocation));
    }

    private void applyFilter() {
        List<Car> filteredCars = new ArrayList<>();

        for (Car car : allCars) {
            String type = normalizeText(car.getType());
            boolean matchesCategory =
                    (currentCategory.equals(CATEGORY_SALE) && isSaleType(type))
                            || (currentCategory.equals(CATEGORY_RENTAL) && isRentalType(type) && !isDriverType(type))
                            || (currentCategory.equals(CATEGORY_DRIVER) && isDriverType(type));

            if (matchesCategory && matchesSelectedBrand(car) && matchesSelectedLocation(car)) {
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