package com.example.doanmb.ui.driver;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.databinding.FragmentDriverPostBinding;
import com.example.doanmb.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/** Tài xế đăng bài cho thuê (type driver / driver_only) — MVVM. */
public class DriverPostFragment extends BaseFragment {

    private FragmentDriverPostBinding binding;
    private DriverPostViewModel viewModel;
    private final List<Uri> selectedImages = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris == null || uris.isEmpty() || !isAdded()) return;
                selectedImages.addAll(uris);
                renderPreviews();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDriverPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DriverPostViewModel.class);

        binding.rgPostType.setOnCheckedChangeListener((group, checkedId) ->
                binding.etPostTitle.setHint(checkedId == R.id.rb_with_car
                        ? "VD: Toyota Vios 2022 kèm tài xế"
                        : "VD: Tài xế 5 năm kinh nghiệm nhận lái thuê"));

        binding.btnPostPickImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        binding.btnPostSubmit.setOnClickListener(v -> submit());

        viewModel.getPrefillCarType().observe(getViewLifecycleOwner(), carType -> {
            if (carType != null && binding.etPostCartype.getText().toString().isEmpty()) {
                binding.etPostCartype.setText(carType);
            }
        });
        viewModel.getPosting().observe(getViewLifecycleOwner(), posting -> {
            boolean p = Boolean.TRUE.equals(posting);
            binding.btnPostSubmit.setEnabled(!p);
            binding.btnPostSubmit.setText(p ? "Đang đăng..." : "Đăng bài");
        });
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
        viewModel.getSuccess().observe(getViewLifecycleOwner(), ok -> { if (Boolean.TRUE.equals(ok)) clearForm(); });

        viewModel.loadPrefill();
    }

    private void submit() {
        DriverPostViewModel.PostInput in = new DriverPostViewModel.PostInput();
        in.title = binding.etPostTitle.getText().toString().trim();
        in.price = binding.etPostPrice.getText().toString().trim();
        in.priceKm = binding.etPostPriceKm.getText().toString().trim();
        in.carType = binding.etPostCartype.getText().toString().trim();
        in.location = binding.etPostLocation.getText().toString().trim();
        in.desc = binding.etPostInfo.getText().toString().trim();
        in.withCar = binding.rgPostType.getCheckedRadioButtonId() == R.id.rb_with_car;
        in.images = new ArrayList<>(selectedImages);
        viewModel.submit(requireContext().getApplicationContext(), in);
    }

    private void renderPreviews() {
        if (binding == null) return;
        binding.layoutPostPreviews.removeAllViews();
        binding.scrollPostPreviews.setVisibility(selectedImages.isEmpty() ? View.GONE : View.VISIBLE);

        int size = dp(110), margin = dp(6);
        for (Uri uri : selectedImages) {
            ImageView iv = new ImageView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(margin);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFEEEEEE);
            Glide.with(this).load(uri).into(iv);
            iv.setOnClickListener(v -> { selectedImages.remove(uri); renderPreviews(); });
            binding.layoutPostPreviews.addView(iv);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void clearForm() {
        binding.etPostTitle.setText("");
        binding.etPostPrice.setText("");
        binding.etPostPriceKm.setText("");
        binding.etPostLocation.setText("");
        binding.etPostInfo.setText("");
        selectedImages.clear();
        renderPreviews();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
