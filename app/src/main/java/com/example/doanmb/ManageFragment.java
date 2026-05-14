package com.example.doanmb;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageFragment extends Fragment {

    // Tab buttons
    private Button btnTabMyPosts, btnTabRequests;
    private LinearLayout layoutMyPosts, layoutRequests;

    // Tab 1: Xe đã đăng
    private RecyclerView rvMyPosts;
    private TextView tvMyPostCount, tvEmptyPosts;
    private ProfileCarAdapter myPostsAdapter;
    private List<Car> myCarList = new ArrayList<>();

    // Tab 2: Yêu cầu nhận được
    private RecyclerView rvRequests;
    private TextView tvRequestCount, tvEmptyRequests;
    private RequestAdapter requestAdapter;
    private List<Map<String, Object>> orderList = new ArrayList<>();
    private List<String> orderIds = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) currentUserId = user.getUid();

        initViews(view);
        setupTabs();
        setupRecyclerViews();

        if (currentUserId != null) {
            loadMyPosts();
            loadRequests();
        }

        return view;
    }

    private void initViews(View view) {
        btnTabMyPosts = view.findViewById(R.id.btnTabMyPosts);
        btnTabRequests = view.findViewById(R.id.btnTabRequests);
        layoutMyPosts = view.findViewById(R.id.layoutMyPosts);
        layoutRequests = view.findViewById(R.id.layoutRequests);
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        rvRequests = view.findViewById(R.id.rvRequests);
        tvMyPostCount = view.findViewById(R.id.tvMyPostCount);
        tvEmptyPosts = view.findViewById(R.id.tvEmptyPosts);
        tvRequestCount = view.findViewById(R.id.tvRequestCount);
        tvEmptyRequests = view.findViewById(R.id.tvEmptyRequests);
    }

    private void setupTabs() {
        btnTabMyPosts.setOnClickListener(v -> showTab(true));
        btnTabRequests.setOnClickListener(v -> showTab(false));
    }

    private void showTab(boolean showPosts) {
        if (showPosts) {
            layoutMyPosts.setVisibility(View.VISIBLE);
            layoutRequests.setVisibility(View.GONE);
            btnTabMyPosts.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabMyPosts.setTextColor(0xFFFFFFFF);
            btnTabRequests.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabRequests.setTextColor(0xFF1976D2);
        } else {
            layoutMyPosts.setVisibility(View.GONE);
            layoutRequests.setVisibility(View.VISIBLE);
            btnTabRequests.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1976D2));
            btnTabRequests.setTextColor(0xFFFFFFFF);
            btnTabMyPosts.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE3F2FD));
            btnTabMyPosts.setTextColor(0xFF1976D2);
        }
    }

    private void setupRecyclerViews() {
        // Tab 1: Xe đã đăng
        rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        myPostsAdapter = new ProfileCarAdapter(myCarList);
        rvMyPosts.setAdapter(myPostsAdapter);

        // Tab 2: Yêu cầu
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        requestAdapter = new RequestAdapter(orderList, orderIds, new RequestAdapter.OnActionListener() {
            @Override
            public void onConfirm(String orderId, String carId, Map<String, Object> order) {
                confirmRequest(orderId, carId);
            }

            @Override
            public void onReject(String orderId, String carId) {
                rejectRequest(orderId, carId);
            }
        });
        rvRequests.setAdapter(requestAdapter);
    }

    // Load xe mình đã đăng
    private void loadMyPosts() {
        db.collection("cars")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    myCarList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String name = doc.getString("name");
                        String price = doc.getString("price");
                        String info = doc.getString("info");
                        String type = doc.getString("type");
                        String status = doc.getString("status");
                        if (name == null) continue;

                        Car car = new Car(name, price != null ? price : "", info != null ? info : "", android.R.drawable.ic_menu_gallery);
                        car.setId(doc.getId());
                        car.setType(type != null ? type : "");

                        // Thêm badge trạng thái vào info nếu đang giữ chỗ
                        if ("holding".equals(status)) {
                            car = new Car("⏳ " + name, price != null ? price : "", "Đang có người đặt cọc • " + (info != null ? info : ""), android.R.drawable.ic_menu_gallery);
                            car.setId(doc.getId());
                            car.setType(type != null ? type : "");
                        }
                        myCarList.add(car);
                    }
                    myPostsAdapter.updateList(myCarList);
                    tvMyPostCount.setText("Tin đã đăng: " + myCarList.size());
                    tvEmptyPosts.setVisibility(myCarList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvMyPosts.setVisibility(myCarList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    // Load yêu cầu mua/thuê xe của mình
    private void loadRequests() {
        db.collection("orders")
                .whereEqualTo("sellerId", currentUserId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    orderList.clear();
                    orderIds.clear();

                    // Nếu không có field sellerId thì query theo carId
                    if (snapshots.isEmpty()) {
                        loadRequestsByCarId();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : snapshots) {
                        orderList.add(doc.getData());
                        orderIds.add(doc.getId());
                    }
                    updateRequestsUI();
                })
                .addOnFailureListener(e -> loadRequestsByCarId());
    }

    // Load yêu cầu dựa trên các carId của mình
    private void loadRequestsByCarId() {
        db.collection("cars")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(carSnapshots -> {
                    List<String> myCarIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : carSnapshots) {
                        myCarIds.add(doc.getId());
                    }

                    if (myCarIds.isEmpty()) {
                        updateRequestsUI();
                        return;
                    }

                    // Lấy tất cả orders rồi lọc theo carId
                    db.collection("orders").get()
                            .addOnSuccessListener(orderSnapshots -> {
                                orderList.clear();
                                orderIds.clear();
                                for (QueryDocumentSnapshot doc : orderSnapshots) {
                                    String carId = doc.getString("carId");
                                    if (carId != null && myCarIds.contains(carId)) {
                                        orderList.add(doc.getData());
                                        orderIds.add(doc.getId());
                                    }
                                }
                                updateRequestsUI();
                            });
                });
    }

    private void updateRequestsUI() {
        requestAdapter.updateList(orderList, orderIds);
        tvRequestCount.setText("Yêu cầu: " + orderList.size());
        tvEmptyRequests.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
        rvRequests.setVisibility(orderList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // Xác nhận yêu cầu → xe chuyển sang "sold/rented", order → confirmed
    private void confirmRequest(String orderId, String carId) {
        // Cập nhật order
        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("status", "confirmed");
        db.collection("orders").document(orderId).update(orderUpdate);

        // Cập nhật trạng thái xe thành "sold" (ẩn khỏi danh sách)
        if (!carId.isEmpty()) {
            Map<String, Object> carUpdate = new HashMap<>();
            carUpdate.put("status", "sold");
            db.collection("cars").document(carId).update(carUpdate)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "✅ Đã xác nhận! Xe sẽ được ẩn khỏi danh sách.", Toast.LENGTH_SHORT).show();
                        loadMyPosts();
                        loadRequests();
                    });
        } else {
            Toast.makeText(getContext(), "✅ Đã xác nhận yêu cầu!", Toast.LENGTH_SHORT).show();
            loadRequests();
        }
    }

    // Từ chối yêu cầu → xe về trạng thái bình thường
    private void rejectRequest(String orderId, String carId) {
        // Cập nhật order
        Map<String, Object> orderUpdate = new HashMap<>();
        orderUpdate.put("status", "rejected");
        db.collection("orders").document(orderId).update(orderUpdate);

        // Đưa xe về trạng thái active
        if (!carId.isEmpty()) {
            Map<String, Object> carUpdate = new HashMap<>();
            carUpdate.put("status", "active");
            db.collection("cars").document(carId).update(carUpdate)
                    .addOnSuccessListener(v -> {
                        Toast.makeText(getContext(), "Đã từ chối yêu cầu. Xe tiếp tục hiển thị.", Toast.LENGTH_SHORT).show();
                        loadMyPosts();
                        loadRequests();
                    });
        } else {
            Toast.makeText(getContext(), "Đã từ chối yêu cầu.", Toast.LENGTH_SHORT).show();
            loadRequests();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadMyPosts();
            loadRequests();
        }
    }
}