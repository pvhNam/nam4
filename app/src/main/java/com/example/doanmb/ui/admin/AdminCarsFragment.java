package com.example.doanmb.ui.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.adapter.CarAdminAdapter;
import com.example.doanmb.databinding.FragmentAdminCarsBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Màn Admin duyệt xe — MVVM: Fragment chỉ hiển thị; lọc & nghiệp vụ ở
 * {@link AdminCarsViewModel}.
 */
public class AdminCarsFragment extends BaseFragment {

    private FragmentAdminCarsBinding binding;
    private AdminCarsViewModel viewModel;
    private CarAdminAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCarsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminCarsViewModel.class);

        adapter = new CarAdminAdapter(new ArrayList<>(), new ArrayList<>(),
                new CarAdminAdapter.OnCarActionListener() {
                    @Override public void onApprove(String carId) { viewModel.approve(carId); }
                    @Override public void onReject(String carId)  { viewModel.reject(carId); }
                    @Override public void onDelete(String carId)  { viewModel.delete(carId); }
                });
        binding.rvCarsAdmin.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvCarsAdmin.setAdapter(adapter);

        binding.btnTabPending.setOnClickListener(v -> switchTab(true));
        binding.btnTabAll.setOnClickListener(v -> switchTab(false));

        viewModel.getCars().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);

        switchTab(true);
    }

    private void switchTab(boolean pending) {
        applyTabStyle(pending);
        viewModel.selectTab(pending);
    }

    private void render(List<DocumentSnapshot> docs) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (docs != null) {
            for (DocumentSnapshot d : docs) { data.add(d.getData()); ids.add(d.getId()); }
        }
        adapter.updateList(data, ids);

        boolean pending = viewModel.isShowingPending();
        binding.tvCarCount.setText(pending ? data.size() + " xe chờ duyệt" : data.size() + " xe");
        binding.tvEmptyCars.setText(pending ? "Không có xe nào chờ duyệt ✓" : "Không có xe nào");
        boolean empty = data.isEmpty();
        binding.tvEmptyCars.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvCarsAdmin.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void applyTabStyle(boolean pendingActive) {
        int active = 0xFFC62828, activeText = 0xFFFFFFFF, inactive = 0xFFFFCDD2, inactiveText = 0xFFC62828;
        binding.btnTabPending.setBackgroundTintList(ColorStateList.valueOf(pendingActive ? active : inactive));
        binding.btnTabPending.setTextColor(pendingActive ? activeText : inactiveText);
        binding.btnTabAll.setBackgroundTintList(ColorStateList.valueOf(pendingActive ? inactive : active));
        binding.btnTabAll.setTextColor(pendingActive ? inactiveText : activeText);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
