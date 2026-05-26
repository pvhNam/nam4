package com.example.doanmb.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.doanmb.R;
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
    private final List<Map<String, Object>> convList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        db = FirebaseFirestore.getInstance();

        layoutNotLoggedIn = view.findViewById(R.id.layout_msg_not_logged_in);
        layoutEmpty       = view.findViewById(R.id.layout_msg_empty);
        rvConversations   = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ConversationAdapter(convList, db);
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

        listener = db.collection("orders")
                .whereEqualTo("buyerId", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    if (!isAdded()) return;

                    convList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("_isSeller", false);
                        convList.add(data);
                    }

                    // Load thêm orders mà user là seller
                    db.collection("orders")
                            .whereEqualTo("sellerId", uid)
                            .get()
                            .addOnSuccessListener(sellerSnaps -> {
                                if (!isAdded()) return;
                                for (QueryDocumentSnapshot doc : sellerSnaps) {
                                    Map<String, Object> data = doc.getData();
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
        if (listener != null) { listener.remove(); listener = null; }
    }

    // ── Adapter ──────────────────────────────────────────────────────
    static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {
        private final List<Map<String, Object>> list;
        private final FirebaseFirestore db;

        ConversationAdapter(List<Map<String, Object>> list, FirebaseFirestore db) {
            this.list = list;
            this.db   = db;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conversation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> item = list.get(position);
            String carName   = getStr(item, "carName");
            String carPrice  = getStr(item, "carPrice");
            String status    = getStr(item, "status");
            boolean isSeller = Boolean.TRUE.equals(item.get("_isSeller"));

            // Tên + uid của đối phương
            String otherUid;
            String displayName;
            if (isSeller) {
                // Mình là seller → hiện thông tin buyer
                otherUid    = getStr(item, "buyerId");
                String renterName = getStr(item, "renterName");
                displayName = renterName.isEmpty() ? "Người mua" : renterName;
            } else {
                // Mình là buyer → hiện thông tin seller
                otherUid    = getStr(item, "sellerId");
                displayName = "Người bán xe";
            }

            h.tvName.setText(displayName);
            h.tvCarName.setText(carName);
            h.tvLastMsg.setText(carPrice);

            // Avatar mặc định — chữ cái đầu với màu theo tên
            String initial = displayName.length() > 0
                    ? String.valueOf(displayName.charAt(0)).toUpperCase() : "?";
            h.tvAvatar.setText(initial);
            h.tvAvatar.setBackgroundColor(getAvatarColor(displayName));
            h.tvAvatar.setVisibility(View.VISIBLE);
            h.ivAvatar.setVisibility(View.GONE);

            // Load avatar thật từ Firestore nếu có uid
            if (!otherUid.isEmpty()) {
                db.collection("users").document(otherUid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc == null || !doc.exists()) return;

                            // Cập nhật tên nếu chưa có
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty() && h.tvName.getText().equals("Người bán xe")) {
                                h.tvName.setText(name);
                                String init = String.valueOf(name.charAt(0)).toUpperCase();
                                h.tvAvatar.setText(init);
                                h.tvAvatar.setBackgroundColor(getAvatarColor(name));
                            }

                            // Load ảnh đại diện
                            String avatarUrl = doc.getString("avatarUrl");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                h.tvAvatar.setVisibility(View.GONE);
                                h.ivAvatar.setVisibility(View.VISIBLE);
                                Glide.with(h.ivAvatar.getContext())
                                        .load(avatarUrl)
                                        .transform(new CircleCrop())
                                        .placeholder(android.R.drawable.ic_menu_gallery)
                                        .into(h.ivAvatar);
                            }
                        });
            }

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

        private String getStr(Map<String, Object> map, String key) {
            Object val = map.get(key);
            return (val instanceof String) ? (String) val : "";
        }

        // Tạo màu avatar từ tên — mỗi tên luôn ra cùng 1 màu
        private int getAvatarColor(String name) {
            int[] colors = {
                    0xFF1976D2, // xanh dương
                    0xFF388E3C, // xanh lá
                    0xFFF57C00, // cam
                    0xFF7B1FA2, // tím
                    0xFFC62828, // đỏ
                    0xFF00838F, // xanh ngọc
                    0xFF5D4037, // nâu
                    0xFF1565C0, // xanh navy
            };
            int index = Math.abs(name.hashCode()) % colors.length;
            return colors[index];
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  tvAvatar, tvName, tvCarName, tvLastMsg, tvStatus;
            ImageView ivAvatar;

            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvConvAvatar);
                ivAvatar  = v.findViewById(R.id.ivConvAvatar);
                tvName    = v.findViewById(R.id.tvConvName);
                tvCarName = v.findViewById(R.id.tvConvCarName);
                tvLastMsg = v.findViewById(R.id.tvConvLastMsg);
                tvStatus  = v.findViewById(R.id.tvConvStatus);
            }
        }
    }
}