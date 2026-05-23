package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doanmb.R;
import com.example.doanmb.ui.activity.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn, layoutEmpty;
    private RecyclerView rvConversations;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private ConversationAdapter adapter;
    private List<Map<String, Object>> convList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        db = FirebaseFirestore.getInstance();

        layoutNotLoggedIn = view.findViewById(R.id.layout_msg_not_logged_in);
        layoutEmpty = view.findViewById(R.id.layout_msg_empty);
        rvConversations = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ConversationAdapter(convList);
        rvConversations.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            rvConversations.setVisibility(View.GONE);
            return;
        }
        layoutNotLoggedIn.setVisibility(View.GONE);
        loadConversations(user.getUid());
    }

    private void loadConversations(String uid) {
        if (listener != null) listener.remove();

        // Lấy tất cả orders liên quan đến user (cả buyer lẫn seller)
        listener = db.collection("orders")
                .whereEqualTo("buyerId", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    if (!isAdded()) return;

                    convList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        convList.add(doc.getData());
                    }

                    // Load thêm orders mà user là seller
                    db.collection("orders")
                            .whereEqualTo("sellerId", uid)
                            .get()
                            .addOnSuccessListener(sellerSnaps -> {
                                if (!isAdded()) return;
                                for (QueryDocumentSnapshot doc : sellerSnaps) {
                                    Map<String, Object> data = doc.getData();
                                    // Đánh dấu để hiển thị đúng vai trò
                                    data.put("_isSeller", true);
                                    convList.add(data);
                                }
                                adapter.notifyDataSetChanged();
                                boolean isEmpty = convList.isEmpty();
                                rvConversations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                                layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                            });
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    // ── Adapter nội bộ ──────────────────────────────────────────────
    static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {
        private List<Map<String, Object>> list;
        ConversationAdapter(List<Map<String, Object>> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> item = list.get(position);
            String carName  = (String) item.get("carName");
            String carPrice = (String) item.get("carPrice");
            String status   = (String) item.get("status");
            String type     = (String) item.get("type");
            boolean isSeller = Boolean.TRUE.equals(item.get("_isSeller"));

            // Tên hiển thị theo vai trò
            String displayName = isSeller
                    ? "👤 " + getOrDefault(item, "renterName", "Người mua")
                    : "🏪 Người bán xe";
            h.tvName.setText(displayName);
            h.tvCarName.setText(carName != null ? carName : "");
            h.tvLastMsg.setText(carPrice != null ? carPrice : "");

            // Avatar chữ cái đầu
            String initial = displayName.length() > 1 ? String.valueOf(displayName.charAt(2)).toUpperCase() : "?";
            h.tvAvatar.setText(initial);

            // Badge trạng thái
            if ("confirmed".equals(status)) {
                h.tvStatus.setText("✅ Xác nhận");
                h.tvStatus.setBackgroundColor(0xFF4CAF50);
            } else if ("rejected".equals(status)) {
                h.tvStatus.setText("❌ Từ chối");
                h.tvStatus.setBackgroundColor(0xFFF44336);
            } else {
                h.tvStatus.setText("⏳ Chờ");
                h.tvStatus.setBackgroundColor(0xFFFF9800);
            }
        }

        private String getOrDefault(Map<String, Object> map, String key, String def) {
            Object val = map.get(key);
            return val instanceof String && !((String) val).isEmpty() ? (String) val : def;
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            android.widget.TextView tvAvatar, tvName, tvCarName, tvLastMsg, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvConvAvatar);
                tvName    = v.findViewById(R.id.tvConvName);
                tvCarName = v.findViewById(R.id.tvConvCarName);
                tvLastMsg = v.findViewById(R.id.tvConvLastMsg);
                tvStatus  = v.findViewById(R.id.tvConvStatus);
            }
        }
    }
}