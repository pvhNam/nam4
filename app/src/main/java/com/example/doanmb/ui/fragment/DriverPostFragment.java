package com.example.doanmb.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tài xế đăng bài cho thuê: "xe có tài xế" (type = driver) hoặc
 * "người lái / lái thuê" (type = driver_only). Cả hai đều lưu vào collection
 * "cars" nên tự hiển thị ở tab "Xe có tài xế" phía người dùng.
 */
public class DriverPostFragment extends Fragment {

    private RadioGroup rgPostType;
    private EditText etTitle, etCarType, etPrice, etLocation, etInfo;
    private ImageView ivPreview;
    private Button btnPickImage, btnSubmit;

    private Uri selectedImage;
    private FirebaseFirestore db;
    private String uid;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !isAdded()) return;
                selectedImage = uri;
                ivPreview.setVisibility(View.VISIBLE);
                Glide.with(this).load(uri).into(ivPreview);
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_driver_post, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";

        rgPostType = view.findViewById(R.id.rg_post_type);
        etTitle = view.findViewById(R.id.et_post_title);
        etCarType = view.findViewById(R.id.et_post_cartype);
        etPrice = view.findViewById(R.id.et_post_price);
        etLocation = view.findViewById(R.id.et_post_location);
        etInfo = view.findViewById(R.id.et_post_info);
        ivPreview = view.findViewById(R.id.iv_post_preview);
        btnPickImage = view.findViewById(R.id.btn_post_pick_image);
        btnSubmit = view.findViewById(R.id.btn_post_submit);

        rgPostType.setOnCheckedChangeListener((group, checkedId) ->
                etTitle.setHint(checkedId == R.id.rb_with_car
                        ? "VD: Toyota Vios 2022 kèm tài xế"
                        : "VD: Tài xế 5 năm kinh nghiệm nhận lái thuê"));

        btnPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitPost());

        prefillCarType();
        return view;
    }

    /** Điền sẵn loại xe từ hồ sơ tài xế cho đỡ phải gõ lại. */
    private void prefillCarType() {
        if (uid.isEmpty()) return;
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            String carType = doc.getString("driverCarType");
            if (carType != null && !carType.isEmpty() && etCarType.getText().toString().isEmpty()) {
                etCarType.setText(carType);
            }
        });
    }

    private void submitPost() {
        String title = etTitle.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (title.isEmpty() || price.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tiêu đề và giá!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uid.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập trước!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang đăng...");

        if (selectedImage != null) {
            CloudinaryHelper.uploadImage(requireContext().getApplicationContext(), selectedImage,
                    new CloudinaryHelper.OnUploadCallback() {
                        @Override
                        public void onSuccess(String imageUrl) {
                            savePost(title, price, imageUrl);
                        }

                        @Override
                        public void onFailure(String error) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Lỗi upload ảnh: " + error, Toast.LENGTH_SHORT).show();
                            resetButton();
                        }
                    });
        } else {
            savePost(title, price, "");
        }
    }

    private void savePost(String title, String price, String imageUrl) {
        boolean withCar = rgPostType.getCheckedRadioButtonId() == R.id.rb_with_car;
        String carType = etCarType.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String desc = etInfo.getText().toString().trim();

        String info = (withCar ? "Xe có tài xế" : "Lái thuê")
                + (carType.isEmpty() ? "" : " • " + carType)
                + (location.isEmpty() ? "" : " • " + location)
                + (desc.isEmpty() ? "" : "\n" + desc);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;

                    String sellerName = doc.getString("name");
                    String sellerPhone = doc.getString("phone");

                    List<String> imageUrls = new ArrayList<>();
                    if (!imageUrl.isEmpty()) imageUrls.add(imageUrl);

                    Map<String, Object> post = new HashMap<>();
                    post.put("name", title);
                    post.put("price", price + " đ/ngày");
                    post.put("info", info);
                    post.put("type", withCar ? "driver" : "driver_only");
                    post.put("brand", "");
                    post.put("location", location);
                    post.put("status", "pending");
                    post.put("imageUrl", imageUrl);
                    post.put("imageUrls", imageUrls);
                    post.put("userId", uid);
                    post.put("sellerId", uid);
                    post.put("sellerName", sellerName != null ? sellerName : "");
                    post.put("sellerPhone", sellerPhone != null ? sellerPhone : "");
                    post.put("createdAt", Timestamp.now());

                    db.collection("cars").add(post)
                            .addOnSuccessListener(ref -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "✅ Đăng bài thành công!", Toast.LENGTH_LONG).show();
                                clearForm();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Lỗi lấy thông tin tài xế!", Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void clearForm() {
        etTitle.setText("");
        etPrice.setText("");
        etLocation.setText("");
        etInfo.setText("");
        selectedImage = null;
        ivPreview.setImageDrawable(null);
        ivPreview.setVisibility(View.GONE);
        resetButton();
    }

    private void resetButton() {
        btnSubmit.setEnabled(true);
        btnSubmit.setText("Đăng bài");
    }
}
