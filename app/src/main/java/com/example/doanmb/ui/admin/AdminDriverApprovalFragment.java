package com.example.doanmb.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.R;
import com.example.doanmb.adapter.DriverApprovalAdapter;
import com.example.doanmb.databinding.FragmentAdminDriverApprovalBinding;
import com.example.doanmb.model.User;
import com.example.doanmb.ui.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/** Màn Admin duyệt hồ sơ tài xế — MVVM. */
public class AdminDriverApprovalFragment extends BaseFragment {

    private FragmentAdminDriverApprovalBinding binding;
    private AdminDriverApprovalViewModel viewModel;
    private DriverApprovalAdapter adapter;
    private final List<User> pendingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDriverApprovalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminDriverApprovalViewModel.class);

        binding.btnDaBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        adapter = new DriverApprovalAdapter(pendingList, new DriverApprovalAdapter.OnDecisionListener() {
            @Override public void onApprove(User u) {}
            @Override public void onReject(User u) {}
        });
        adapter.setOnItemClickListener(u -> {
            AdminDriverDetailFragment detail = AdminDriverDetailFragment.newInstance(u.getUid());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, detail)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvDriverApproval.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDriverApproval.setAdapter(adapter);

        viewModel.getPending().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);

        viewModel.loadPending();
    }

    private void render(List<User> users) {
        pendingList.clear();
        if (users != null) pendingList.addAll(users);
        adapter.notifyDataSetChanged();
        int count = pendingList.size();
        binding.tvDaPendingCount.setText(count + " hồ sơ đang chờ duyệt");
        binding.tvDaEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        binding.rvDriverApproval.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadPending();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
