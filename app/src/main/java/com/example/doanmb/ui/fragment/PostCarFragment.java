package com.example.doanmb.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PostCarFragment extends Fragment {

    public interface OnPostCarSubmittedListener {
        void onPostCarSubmitted();
    }

    private EditText etCarName, etCarPrice, etCarInfo, etCarYear, etCarKm, etCarLocation;
    private Spinner spinnerBrand, spinnerType;
    private Button btnSubmitPost, btnPickImage, btnRemoveImage;
    private View layoutUploadPrompt;
    private ImageView ivPreview;
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isViewReady()) return;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(ivPreview);
                    ivPreview.setVisibility(View.VISIBLE);
                    layoutUploadPrompt.setVisibility(View.GONE);
                    btnRemoveImage.setVisibility(View.VISIBLE);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isViewReady()) return;
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(requireContext(), "Bạn cần cấp quyền để chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_car, container, false);

        initViews(view);
        setupSpinners();
        setupActions();

        return view;
    }

    private void initViews(View view) {
        etCarName = view.findViewById(R.id.et_car_name);
        etCarPrice = view.findViewById(R.id.et_car_price);
        etCarInfo = view.findViewById(R.id.et_car_info);
        etCarYear = view.findViewById(R.id.et_car_year);
        etCarKm = view.findViewById(R.id.et_car_km);
        etCarLocation = view.findViewById(R.id.et_car_location);
        spinnerBrand = view.findViewById(R.id.spinner_brand);
        spinnerType = view.findViewById(R.id.spinner_type);
        btnSubmitPost = view.findViewById(R.id.btn_submit_post);
        btnPickImage = view.findViewById(R.id.btn_pick_image);
        btnRemoveImage = view.findViewById(R.id.btn_remove_image);
        layoutUploadPrompt = view.findViewById(R.id.layout_upload_prompt);
        ivPreview = view.findViewById(R.id.iv_car_preview);
    }

    private void setupActions() {
        btnPickImage.setOnClickListener(v -> pickImage());
        btnRemoveImage.setOnClickListener(v -> clearSelectedImage());
        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void setupSpinners() {
        String[] brands = {"Toyota", "Honda", "Mazda", "Kia", "Ford", "Hyundai", "VinFast", "Mitsubishi", "Suzuki", "Nissan", "Mercedes", "BMW", "Audi", "Khác"};
        spinnerBrand.setAdapter(makeAdapter(brands));

        String[] types = {"Cần Bán", "Cho Thuê"};
        spinnerType.setAdapter(makeAdapter(types));

    }

    private ArrayAdapter<String> makeAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void pickImage() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void submitPost() {
        String name = etCarName.getText().toString().trim();
        String price = etCarPrice.getText().toString().trim();
        String info = etCarInfo.getText().toString().trim();
        String year = etCarYear.getText().toString().trim();
        String km = etCarKm.getText().toString().trim();
        String location = etCarLocation.getText().toString().trim();
        String brand = spinnerBrand.getSelectedItem().toString();
        String typeLabel = spinnerType.getSelectedItem().toString();
        String type = typeLabel.equals("Cần Bán") ? "sale" : "rental";

        if (name.isEmpty() || price.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên xe và giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập trước!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullInfo = brand
                + (year.isEmpty() ? "" : " • " + year)
                + (km.isEmpty() ? "" : " • " + km + " km")
                + (location.isEmpty() ? "" : " • " + location)
                + (info.isEmpty() ? "" : "\n" + info);

        btnSubmitPost.setEnabled(false);
        btnSubmitPost.setText("Đang đăng...");

        if (selectedImageUri != null) {
            CloudinaryHelper.uploadImage(
                    requireContext().getApplicationContext(),
                    selectedImageUri,
                    new CloudinaryHelper.OnUploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            saveCar(user, name, price, fullInfo, type, brand,
                                    year, km, location, imageUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            if (!isViewReady()) return;
                            Toast.makeText(requireContext(), "Lỗi upload ảnh: " + error, Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    }
            );
        } else {
            saveCar(user, name, price, fullInfo, type, brand,
                    year, km, location, "");
        }
    }

    private void saveCar(FirebaseUser user, String name, String price, String fullInfo,
                         String type, String brand, String year, String km,
                         String location, String imageUrl) {
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isViewReady()) return;

                    String sellerName = doc.getString("name");
                    String sellerPhone = doc.getString("phone");

                    Map<String, Object> car = new HashMap<>();
                    car.put("name", name);
                    car.put("price", price + (type.equals("rental") ? " đ/ngày" : " VNĐ"));
                    car.put("info", fullInfo);
                    car.put("type", type);
                    car.put("brand", brand);
                    car.put("fuel", "");
                    car.put("condition", "");
                    car.put("transmission", "");
                    car.put("year", year);
                    car.put("km", km);
                    car.put("location", location);
                    car.put("status", "pending");
                    car.put("imageUrl", imageUrl);
                    car.put("userId", user.getUid());
                    car.put("sellerId", user.getUid());
                    car.put("sellerName", sellerName != null ? sellerName : "");
                    car.put("sellerPhone", sellerPhone != null ? sellerPhone : "");
                    car.put("createdAt", com.google.firebase.Timestamp.now());

                    FirebaseFirestore.getInstance().collection("cars").add(car)
                            .addOnSuccessListener(ref -> {
                                if (!isViewReady()) return;
                                Toast.makeText(requireContext(), "Đăng tin thành công! Tin đang chờ admin duyệt.", Toast.LENGTH_LONG).show();
                                clearForm();
                                notifyPostSubmitted();
                            })
                            .addOnFailureListener(e -> {
                                if (!isViewReady()) return;
                                Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isViewReady()) return;
                    Toast.makeText(requireContext(), "Lỗi lấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void clearForm() {
        etCarName.setText("");
        etCarPrice.setText("");
        etCarInfo.setText("");
        etCarYear.setText("");
        etCarKm.setText("");
        etCarLocation.setText("");
        spinnerBrand.setSelection(0);
        spinnerType.setSelection(0);
        clearSelectedImage();
        resetButton();
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        ivPreview.setImageDrawable(null);
        ivPreview.setVisibility(View.GONE);
        layoutUploadPrompt.setVisibility(View.VISIBLE);
        btnRemoveImage.setVisibility(View.GONE);
    }

    private void resetButton() {
        btnSubmitPost.setEnabled(true);
        btnSubmitPost.setText("Gửi yêu cầu đăng tin");
    }

    private void notifyPostSubmitted() {
        Fragment parent = getParentFragment();
        if (parent instanceof OnPostCarSubmittedListener) {
            ((OnPostCarSubmittedListener) parent).onPostCarSubmitted();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private boolean isViewReady() {
        return isAdded() && getView() != null && btnSubmitPost != null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        etCarName = null;
        etCarPrice = null;
        etCarInfo = null;
        etCarYear = null;
        etCarKm = null;
        etCarLocation = null;
        spinnerBrand = null;
        spinnerType = null;
        btnSubmitPost = null;
        btnPickImage = null;
        btnRemoveImage = null;
        layoutUploadPrompt = null;
        ivPreview = null;
    }
}