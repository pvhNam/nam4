package com.example.doanmb.ui.customer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.R;
import com.example.doanmb.adapter.ProfileCarAdapter;
import com.example.doanmb.adapter.RequestAdapter;
import com.example.doanmb.databinding.FragmentManageBinding;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.base.BaseFragment;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Tab "Quản lý": xe đã đăng + yêu cầu nhận được — MVVM. */
public class ManageFragment extends BaseFragment {

    private FragmentManageBinding binding;
    private ManageViewModel viewModel;
    private ProfileCarAdapter myPostsAdapter;
    private RequestAdapter requestAdapter;
    private final List<Car> myCarList = new ArrayList<>();
    private final List<Map<String, Object>> orderList = new ArrayList<>();
    private final List<String> orderIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ManageViewModel.class);

        binding.cardTabPosts.setOnClickListener(v -> showTab(true));
        binding.cardTabRequests.setOnClickListener(v -> showTab(false));
        showTab(true);

        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        myPostsAdapter = new ProfileCarAdapter(myCarList, this::openCarDetail);
        binding.rvMyPosts.setAdapter(myPostsAdapter);

        binding.rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestAdapter = new RequestAdapter(orderList, orderIds, new RequestAdapter.OnActionListener() {
            @Override public void onConfirm(String orderId, String carId, Map<String, Object> order) {
                viewModel.confirmRequest(orderId, carId);
            }
            @Override public void onReject(String orderId, String carId) {
                viewModel.rejectRequest(orderId, carId);
            }
        });
        binding.rvRequests.setAdapter(requestAdapter);

        viewModel.getMyPosts().observe(getViewLifecycleOwner(), this::renderPosts);
        viewModel.getRequests().observe(getViewLifecycleOwner(), this::renderRequests);
        viewModel.getMessage().observe(getViewLifecycleOwner(), this::toast);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.start();
    }

    private void renderPosts(List<Car> cars) {
        myCarList.clear();
        if (cars != null) myCarList.addAll(cars);
        myPostsAdapter.updateList(myCarList);
        binding.tvMyPostCount.setText("Tin đã đăng: " + myCarList.size());
        boolean empty = myCarList.isEmpty();
        binding.tvEmptyPosts.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvMyPosts.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderRequests(List<DocumentSnapshot> docs) {
        orderList.clear();
        orderIds.clear();
        if (docs != null) {
            for (DocumentSnapshot d : docs) { orderList.add(d.getData()); orderIds.add(d.getId()); }
        }
        requestAdapter.updateList(orderList, orderIds);
        binding.tvRequestCount.setText("Yêu cầu: " + orderList.size());
        boolean empty = orderList.isEmpty();
        binding.tvEmptyRequests.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvRequests.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showTab(boolean showPosts) {
        binding.layoutMyPosts.setVisibility(showPosts ? View.VISIBLE : View.GONE);
        binding.layoutRequests.setVisibility(showPosts ? View.GONE : View.VISIBLE);
        setTabSelected(binding.tabPostsContent, binding.tvTabPosts, showPosts);
        setTabSelected(binding.tabRequestsContent, binding.tvTabRequests, !showPosts);
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

    private void openCarDetail(Car car) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), CarDetailActivity.class);
        intent.putExtra("CAR_DATA", car);
        intent.putExtra("CAR_ID", car.getId());
        intent.putExtra("SELLER_ID", car.getSellerId());
        intent.putExtra("CAR_TYPE", car.getType());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
