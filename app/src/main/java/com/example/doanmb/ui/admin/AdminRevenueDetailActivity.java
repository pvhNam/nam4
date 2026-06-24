package com.example.doanmb.ui.admin;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.doanmb.adapter.RevenueItemAdapter;
import com.example.doanmb.databinding.ActivityAdminRevenueDetailBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Màn chi tiết doanh thu — MVVM: số liệu ở {@link AdminRevenueDetailViewModel}. */
public class AdminRevenueDetailActivity extends AppCompatActivity {

    private ActivityAdminRevenueDetailBinding binding;
    private AdminRevenueDetailViewModel viewModel;
    private RevenueItemAdapter adapter;

    private List<DocumentSnapshot> orderDocs = new ArrayList<>();
    private List<DocumentSnapshot> carDocs   = new ArrayList<>();
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminRevenueDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        viewModel = new ViewModelProvider(this).get(AdminRevenueDetailViewModel.class);

        adapter = new RevenueItemAdapter();
        binding.rvRevenueDetail.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRevenueDetail.setAdapter(adapter);

        binding.btnRevenueBack.setOnClickListener(v -> finish());
        binding.btnRevTabCommission.setOnClickListener(v -> switchTab(0));
        binding.btnRevTabPosting.setOnClickListener(v -> switchTab(1));

        viewModel.getLoading().observe(this, loading -> {
            boolean l = Boolean.TRUE.equals(loading);
            binding.progressRevenue.setVisibility(l ? View.VISIBLE : View.GONE);
            if (l) { binding.rvRevenueDetail.setVisibility(View.GONE); binding.tvRevenueEmpty.setVisibility(View.GONE); }
        });
        viewModel.getData().observe(this, this::render);

        applyTabStyle(0);
        viewModel.load();
    }

    private void render(AdminRevenueDetailViewModel.Data d) {
        if (d == null) return;
        orderDocs = d.orders;
        carDocs = d.cars;
        binding.tvRevenueGrandTotal.setText(fmt(d.grandTotal));
        binding.tvCommissionTotal.setText(fmt(d.commissionTotal));
        binding.tvPostingTotal.setText(fmt(d.postingTotal));
        renderTab();
    }

    private void switchTab(int tab) {
        currentTab = tab;
        applyTabStyle(tab);
        renderTab();
    }

    private void renderTab() {
        List<Object> items = currentTab == 0 ? buildCommissionItems() : buildPostingItems();
        boolean empty = items.isEmpty();
        binding.tvRevenueEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvRevenueDetail.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) adapter.setItems(items);
    }

    private List<Object> buildCommissionItems() {
        List<DocumentSnapshot> sorted = new ArrayList<>(orderDocs);
        sorted.sort((a, b) -> {
            Timestamp ta = a.getTimestamp("createdAt");
            Timestamp tb = b.getTimestamp("createdAt");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return Long.compare(tb.getSeconds(), ta.getSeconds());
        });

        List<Object> items = new ArrayList<>();
        SimpleDateFormat monthFmt = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        SimpleDateFormat dateFmt  = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String lastMonth = "";

        for (DocumentSnapshot doc : sorted) {
            Timestamp ts = doc.getTimestamp("createdAt");
            String monthStr = ts != null ? "Tháng " + monthFmt.format(ts.toDate()) : "Không rõ ngày";
            if (!monthStr.equals(lastMonth)) { items.add(monthStr); lastMonth = monthStr; }

            String type    = doc.getString("type");
            String carName = doc.getString("carName");
            String buyer   = doc.getString("buyerName");
            if (buyer == null) buyer = doc.getString("renterName");
            if (buyer == null) buyer = "--";
            String dateStr = ts != null ? dateFmt.format(ts.toDate()) : "--";
            boolean isRental = "Thuê xe".equals(type);

            items.add(new RevenueItemAdapter.Entry(
                    carName != null ? carName : "Xe không tên",
                    (isRental ? "Người thuê: " : "Người mua: ") + buyer,
                    fmt(viewModel.commissionOf(doc)),
                    dateStr,
                    isRental ? "Thuê" : "Mua",
                    isRental ? 1 : 0));
        }
        return items;
    }

    private List<Object> buildPostingItems() {
        List<Object> items = new ArrayList<>();
        if (!carDocs.isEmpty()) items.add("Tất cả bài đăng  (" + carDocs.size() + " bài)");
        for (DocumentSnapshot doc : carDocs) {
            String carName    = doc.getString("name");
            String sellerName = doc.getString("sellerName");
            String status     = doc.getString("status");
            if (sellerName == null) sellerName = "--";
            items.add(new RevenueItemAdapter.Entry(
                    carName != null ? carName : "Xe không tên",
                    "Người đăng: " + sellerName,
                    "200.000 VNĐ",
                    statusLabel(status),
                    "Đăng",
                    2));
        }
        return items;
    }

    private String statusLabel(String status) {
        if ("active".equals(status))   return "Đang bán";
        if ("sold".equals(status))     return "Đã bán";
        if ("rejected".equals(status)) return "Từ chối";
        if ("holding".equals(status))  return "Đặt cọc";
        return "Chờ duyệt";
    }

    private String fmt(long amount) {
        if (amount == 0) return "0 VNĐ";
        if (amount >= 1_000_000_000L) return String.format("%.1f tỷ", amount / 1_000_000_000.0);
        if (amount >= 1_000_000L)     return String.format("%.0f triệu", amount / 1_000_000.0);
        return (amount / 1_000) + "K VNĐ";
    }

    private void applyTabStyle(int active) {
        int on = 0xFFC62828, onText = 0xFFFFFFFF, off = 0xFFFFCDD2, offText = 0xFFC62828;
        binding.btnRevTabCommission.setBackgroundTintList(ColorStateList.valueOf(active == 0 ? on : off));
        binding.btnRevTabCommission.setTextColor(active == 0 ? onText : offText);
        binding.btnRevTabPosting.setBackgroundTintList(ColorStateList.valueOf(active == 1 ? on : off));
        binding.btnRevTabPosting.setTextColor(active == 1 ? onText : offText);
    }
}
