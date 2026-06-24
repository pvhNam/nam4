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

import com.example.doanmb.adapter.ReportAdminAdapter;
import com.example.doanmb.databinding.FragmentAdminReportsBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Màn Admin xử lý khiếu nại — MVVM. */
public class AdminReportsFragment extends BaseFragment {

    private FragmentAdminReportsBinding binding;
    private AdminReportsViewModel viewModel;
    private ReportAdminAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminReportsViewModel.class);

        adapter = new ReportAdminAdapter(new ArrayList<>(), new ArrayList<>(),
                new ReportAdminAdapter.OnReportActionListener() {
                    @Override public void onResolve(String reportId) { viewModel.resolve(reportId); }
                    @Override public void onDismiss(String reportId) { viewModel.dismiss(reportId); }
                });
        binding.rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvReports.setAdapter(adapter);

        binding.btnTabReportPending.setOnClickListener(v -> switchTab(true));
        binding.btnTabReportAll.setOnClickListener(v -> switchTab(false));

        viewModel.getReports().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);

        applyTabStyle(true);
        viewModel.load();
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
        binding.tvReportCount.setText(pending ? data.size() + " chờ xử lý" : data.size() + " khiếu nại");
        binding.tvEmptyReports.setText(pending ? "Không có khiếu nại nào chờ xử lý" : "Chưa có khiếu nại nào");
        boolean empty = data.isEmpty();
        binding.tvEmptyReports.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvReports.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void applyTabStyle(boolean pendingActive) {
        int active = 0xFFC62828, activeText = 0xFFFFFFFF, inactive = 0xFFFFCDD2, inactiveText = 0xFFC62828;
        binding.btnTabReportPending.setBackgroundTintList(ColorStateList.valueOf(pendingActive ? active : inactive));
        binding.btnTabReportPending.setTextColor(pendingActive ? activeText : inactiveText);
        binding.btnTabReportAll.setBackgroundTintList(ColorStateList.valueOf(pendingActive ? inactive : active));
        binding.btnTabReportAll.setTextColor(pendingActive ? inactiveText : activeText);
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
