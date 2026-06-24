package com.example.doanmb.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.adapter.UserAdminAdapter;
import com.example.doanmb.databinding.FragmentAdminUsersBinding;
import com.example.doanmb.ui.base.BaseFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Màn Admin quản lý người dùng — MVVM. */
public class AdminUsersFragment extends BaseFragment {

    private FragmentAdminUsersBinding binding;
    private AdminUsersViewModel viewModel;
    private UserAdminAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminUsersViewModel.class);

        adapter = new UserAdminAdapter(new ArrayList<>(), new ArrayList<>(),
                (userId, newRole) -> viewModel.changeRole(userId, newRole),
                (userId, userName, amount) -> viewModel.topUp(userId, userName, amount));
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUsers.setAdapter(adapter);

        viewModel.getUsers().observe(getViewLifecycleOwner(), this::render);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);

        viewModel.load();
    }

    private void render(List<DocumentSnapshot> docs) {
        List<Map<String, Object>> data = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (docs != null) {
            for (DocumentSnapshot d : docs) { data.add(d.getData()); ids.add(d.getId()); }
        }
        adapter.updateList(data, ids);
        binding.tvUserCount.setText(data.size() + " người dùng");
        boolean empty = data.isEmpty();
        binding.tvEmptyUsers.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
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
