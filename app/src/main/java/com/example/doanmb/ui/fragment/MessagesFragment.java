package com.example.doanmb.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.doanmb.adapter.ShortcutAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.ui.activity.ChatDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn, layoutEmpty, layoutContent;
    private RecyclerView rvConversations, rvShortcuts;
    private EditText etSearch;
    private TextView tvGreeting, tvEmptyHint;
    private ImageView imgAvatar;

    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private ConversationAdapter adapter;
    private ShortcutAdapter shortcutAdapter;

    // Toàn bộ danh sách hội thoại gốc (chưa lọc)
    private final List<Map<String, Object>> convList     = new ArrayList<>();
    // Danh sách hiện lên RecyclerView (đã lọc)
    private final List<Map<String, Object>> filteredList = new ArrayList<>();
    private final List<Map<String, Object>> shortcutList = new ArrayList<>();

    // Đang tìm kiếm trong tin nhắn hay không
    private boolean isSearchingMessages = false;
    // Kết quả tìm kiếm trong nội dung tin nhắn:
    // mỗi item = map hội thoại + thêm key "matchedMessage" (nội dung tin nhắn khớp)
    private final List<Map<String, Object>> messageSearchResults = new ArrayList<>();

    private android.widget.FrameLayout frameMsgContent;
    private LinearLayout layoutChatTabContent, layoutNotificationTabContent;
    private androidx.cardview.widget.CardView tabChat, tabNotification;
    private LinearLayout contentTabChat, contentTabNotification;
    private TextView tvTabChat, tvTabNotification;
    private boolean isChatTabActive = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        db = FirebaseFirestore.getInstance();

        layoutNotLoggedIn = view.findViewById(R.id.layout_msg_not_logged_in);
        layoutEmpty       = view.findViewById(R.id.layout_msg_empty);
        layoutContent     = view.findViewById(R.id.layout_msg_content);
        etSearch          = view.findViewById(R.id.et_search_chat);
        tvGreeting        = view.findViewById(R.id.tv_msg_greeting);
        imgAvatar         = view.findViewById(R.id.img_msg_avatar);
        tvEmptyHint       = view.findViewById(R.id.layout_msg_empty) != null
                ? view.findViewById(R.id.layout_msg_empty).findViewById(android.R.id.text1)
                : null;

        rvConversations = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvConversations.setNestedScrollingEnabled(false);
        adapter = new ConversationAdapter(filteredList, db);
        rvConversations.setAdapter(adapter);

        rvShortcuts = view.findViewById(R.id.rv_shortcuts);
        rvShortcuts.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvShortcuts.setNestedScrollingEnabled(false);
        shortcutAdapter = new ShortcutAdapter(shortcutList);
        rvShortcuts.setAdapter(shortcutAdapter);

        setupSearch();

        frameMsgContent              = view.findViewById(R.id.frame_msg_content);
        layoutChatTabContent         = view.findViewById(R.id.layout_chat_tab_content);
        layoutNotificationTabContent = view.findViewById(R.id.layout_notification_tab_content);
        tabChat                       = view.findViewById(R.id.tab_chat);
        tabNotification               = view.findViewById(R.id.tab_notification);
        contentTabChat                = view.findViewById(R.id.content_tab_chat);
        contentTabNotification        = view.findViewById(R.id.content_tab_notification);
        tvTabChat                      = view.findViewById(R.id.tv_tab_chat);
        tvTabNotification              = view.findViewById(R.id.tv_tab_notification);

        tabChat.setOnClickListener(v -> selectTab(true));
        tabNotification.setOnClickListener(v -> selectTab(false));
        return view;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Search  —  tìm theo tên cuộc trò chuyện  VÀ  nội dung tin nhắn
    // ══════════════════════════════════════════════════════════════════════════
    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Quay lại hiển thị tất cả hội thoại
                    isSearchingMessages = false;
                    messageSearchResults.clear();
                    showAllConversations();
                } else {
                    searchEverything(query);
                }
            }
        });
    }

    /**
     * Tìm kiếm đa năng:
     * 1. Lọc hội thoại theo tên xe hoặc tên đối phương (instant, local)
     * 2. Tìm trong nội dung tin nhắn của từng phòng chat (Firestore query)
     * Kết quả 2 loại gộp lại không trùng, hiển thị cùng một danh sách.
     */
    private void searchEverything(String query) {
        String lower = query.toLowerCase();

        // ── Bước 1: lọc local theo tên ────────────────────────────────────
        List<Map<String, Object>> nameMatches = new ArrayList<>();
        for (Map<String, Object> item : convList) {
            String carName     = String.valueOf(item.getOrDefault("carName", "")).toLowerCase();
            String partnerName = String.valueOf(item.getOrDefault("partnerName", "")).toLowerCase();
            if (carName.contains(lower) || partnerName.contains(lower)) {
                nameMatches.add(item);
            }
        }

        // ── Bước 2: tìm trong nội dung tin nhắn Firestore ─────────────────
        // Chạy song song cho tất cả roomId
        messageSearchResults.clear();
        isSearchingMessages = true;

        if (convList.isEmpty()) {
            // Chưa có hội thoại nào → chỉ hiện kết quả tên
            mergeAndShow(nameMatches);
            return;
        }

        final int[] remaining = {convList.size()};
        final List<Map<String, Object>> msgMatches = new ArrayList<>();

        for (Map<String, Object> conv : convList) {
            String roomId = String.valueOf(conv.getOrDefault("roomId", ""));
            if (roomId.isEmpty()) {
                decrementAndMerge(remaining, nameMatches, msgMatches);
                continue;
            }

            db.collection("chat_rooms").document(roomId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(200) // giới hạn để không quá nặng
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        if (!isAdded()) return;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            // Bỏ qua tin bị thu hồi
                            Boolean recalled = doc.getBoolean("recalled");
                            if (Boolean.TRUE.equals(recalled)) continue;

                            String content = doc.getString("content");
                            if (content != null && content.toLowerCase().contains(lower)) {
                                // Tìm thấy tin nhắn khớp trong phòng này
                                // Thêm conv vào kết quả nếu chưa có (tránh trùng)
                                Map<String, Object> resultItem = new HashMap<>(conv);
                                // Gắn đoạn tin nhắn khớp để hiển thị preview
                                resultItem.put("matchedMessage", content);
                                synchronized (msgMatches) {
                                    // Kiểm tra trùng roomId
                                    boolean alreadyAdded = false;
                                    for (Map<String, Object> m : msgMatches) {
                                        if (roomId.equals(m.get("roomId"))) {
                                            alreadyAdded = true;
                                            break;
                                        }
                                    }
                                    if (!alreadyAdded) msgMatches.add(resultItem);
                                }
                                break; // chỉ cần 1 tin khớp mỗi phòng
                            }
                        }
                        decrementAndMerge(remaining, nameMatches, msgMatches);
                    })
                    .addOnFailureListener(e -> decrementAndMerge(remaining, nameMatches, msgMatches));
        }
    }

    private void decrementAndMerge(int[] remaining,
                                   List<Map<String, Object>> nameMatches,
                                   List<Map<String, Object>> msgMatches) {
        synchronized (remaining) {
            remaining[0]--;
            if (remaining[0] <= 0) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> mergeAndShow(nameMatches, msgMatches));
                }
            }
        }
    }

    /** Gộp kết quả tên + kết quả tin nhắn (không trùng roomId) rồi hiển thị */
    private void mergeAndShow(List<Map<String, Object>> nameMatches,
                              List<Map<String, Object>> msgMatches) {
        List<Map<String, Object>> merged = new ArrayList<>(nameMatches);
        for (Map<String, Object> m : msgMatches) {
            String roomId = String.valueOf(m.getOrDefault("roomId", ""));
            boolean exists = false;
            for (Map<String, Object> n : nameMatches) {
                if (roomId.equals(n.getOrDefault("roomId", ""))) { exists = true; break; }
            }
            if (!exists) merged.add(m);
        }
        showFiltered(merged, etSearch.getText().toString().trim());
    }

    /** Overload cho khi chỉ có nameMatches (convList rỗng) */
    private void mergeAndShow(List<Map<String, Object>> nameMatches) {
        showFiltered(nameMatches, etSearch.getText().toString().trim());
    }

    private void showAllConversations() {
        filteredList.clear();
        filteredList.addAll(convList);
        adapter.setSearchQuery("");
        adapter.notifyDataSetChanged();
        updateEmptyState(false, "");
    }

    private void showFiltered(List<Map<String, Object>> results, String query) {
        filteredList.clear();
        filteredList.addAll(results);
        adapter.setSearchQuery(query);
        adapter.notifyDataSetChanged();
        updateEmptyState(filteredList.isEmpty(), query);
    }

    private void updateEmptyState(boolean empty, String query) {
        if (layoutEmpty == null) return;
        if (empty && !query.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (layoutNotLoggedIn != null) layoutNotLoggedIn.setVisibility(View.VISIBLE);
            if (layoutContent != null)     layoutContent.setVisibility(View.GONE);
            if (layoutEmpty != null)       layoutEmpty.setVisibility(View.GONE);
            return;
        }
        if (layoutNotLoggedIn != null) layoutNotLoggedIn.setVisibility(View.GONE);
        if (layoutContent != null)     layoutContent.setVisibility(View.VISIBLE);

        loadUserProfile(user.getUid());
        loadConversations(user.getUid());
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String name      = doc.getString("name");
                String avatarUrl = doc.getString("avatarUrl");
                if (tvGreeting != null)
                    tvGreeting.setText("Hi, " + (name != null ? name : "User"));
                if (avatarUrl != null && !avatarUrl.isEmpty() && imgAvatar != null) {
                    Glide.with(this).load(avatarUrl).into(imgAvatar);
                }
            }
        });
    }

    private void loadConversations(String uid) {
        if (listener != null) listener.remove();

        listener = db.collection("chat_rooms")
                .whereArrayContains("participants", uid)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || !isAdded()) return;

                    convList.clear();
                    shortcutList.clear();
                    Map<String, Boolean> addedPartners = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = new HashMap<>(doc.getData());
                        data.put("roomId", doc.getId());

                        String buyerId   = (String) data.get("buyerId");
                        String sellerId  = (String) data.get("sellerId");
                        String partnerId = uid.equals(buyerId) ? sellerId : buyerId;

                        // Load tên + avatar đối phương
                        if (partnerId != null) {
                            db.collection("users").document(partnerId)
                                    .get().addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists() && isAdded()) {
                                            String name   = userDoc.getString("name");
                                            String avatar = userDoc.getString("avatarUrl");
                                            data.put("partnerName",   name);
                                            data.put("partnerAvatar", avatar);

                                            if (!addedPartners.containsKey(partnerId)
                                                    && shortcutList.size() < 10) {
                                                shortcutList.add(new HashMap<>(data));
                                                addedPartners.put(partnerId, true);
                                                shortcutAdapter.notifyDataSetChanged();
                                            }

                                            // Nếu đang tìm kiếm thì re-run search
                                            String q = etSearch != null
                                                    ? etSearch.getText().toString().trim() : "";
                                            if (!q.isEmpty()) {
                                                searchEverything(q);
                                            } else {
                                                adapter.notifyDataSetChanged();
                                                showAllConversations();
                                            }
                                        }
                                    });
                        }
                        convList.add(data);
                    }

                    // Hiển thị ngay với dữ liệu hiện có
                    String q = etSearch != null ? etSearch.getText().toString().trim() : "";
                    if (q.isEmpty()) showAllConversations();
                    else searchEverything(q);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ConversationAdapter  —  hỗ trợ highlight từ khoá + preview tin nhắn khớp
    // ══════════════════════════════════════════════════════════════════════════
    static class ConversationAdapter
            extends RecyclerView.Adapter<ConversationAdapter.VH> {

        private final List<Map<String, Object>> list;
        private final FirebaseFirestore db;
        private final String currentUid;
        private String searchQuery = "";

        ConversationAdapter(List<Map<String, Object>> list, FirebaseFirestore db) {
            this.list       = list;
            this.db         = db;
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        }

        void setSearchQuery(String q) { this.searchQuery = q != null ? q : ""; }

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

            String carName      = String.valueOf(item.getOrDefault("carName",      ""));
            String partnerName  = String.valueOf(item.getOrDefault("partnerName",  "Đang tải..."));
            String partnerAvatar= String.valueOf(item.getOrDefault("partnerAvatar",""));
            String matchedMsg   = (String) item.get("matchedMessage"); // null nếu tìm theo tên
            String lastMsg      = matchedMsg != null ? matchedMsg
                    : String.valueOf(item.getOrDefault("lastMessage", ""));

            h.tvCarName.setText(carName);
            h.tvName.setText(partnerName);

            // Preview: nếu có tin nhắn khớp → hiện nó, ngược lại hiện lastMessage
            if (lastMsg.isEmpty()) lastMsg = "Bắt đầu trò chuyện...";
            h.tvLastMsg.setText(lastMsg);

            // Avatar
            if (!partnerAvatar.isEmpty() && !"null".equals(partnerAvatar)) {
                h.tvAvatar.setVisibility(View.GONE);
                h.ivAvatar.setVisibility(View.VISIBLE);
                Glide.with(h.ivAvatar.getContext())
                        .load(partnerAvatar)
                        .transform(new CircleCrop())
                        .into(h.ivAvatar);
            } else if (!partnerName.isEmpty() && !"Đang tải...".equals(partnerName)) {
                h.tvAvatar.setVisibility(View.VISIBLE);
                h.ivAvatar.setVisibility(View.GONE);
                h.tvAvatar.setText(String.valueOf(partnerName.charAt(0)).toUpperCase());
            } else {
                h.tvAvatar.setVisibility(View.VISIBLE);
                h.ivAvatar.setVisibility(View.GONE);
                h.tvAvatar.setText("?");
            }

            // Click → mở ChatDetailActivity
            h.itemView.setOnClickListener(v -> {
                String roomId    = String.valueOf(item.getOrDefault("roomId",   ""));
                String buyerId   = String.valueOf(item.getOrDefault("buyerId",  ""));
                String sellerId  = String.valueOf(item.getOrDefault("sellerId", ""));
                String partnerId = currentUid.equals(buyerId) ? sellerId : buyerId;
                String carPrice  = String.valueOf(item.getOrDefault("carPrice", ""));
                String carImage  = String.valueOf(item.getOrDefault("carImage", ""));
                String carId     = String.valueOf(item.getOrDefault("carId",    ""));
                String carType   = String.valueOf(item.getOrDefault("carType",  "sale"));

                Intent intent = new Intent(v.getContext(), ChatDetailActivity.class);
                intent.putExtra("ROOM_ID",      roomId);
                intent.putExtra("PARTNER_ID",   partnerId);
                intent.putExtra("PARTNER_NAME", partnerName);

                Car car = new Car(carName, carPrice, "", 0);
                car.setId(carId);
                car.setImageUrl(carImage);
                car.setSellerId(sellerId);
                car.setType(carType);
                intent.putExtra("CAR_DATA", car);
                v.getContext().startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  tvAvatar, tvName, tvCarName, tvLastMsg;
            ImageView ivAvatar;

            VH(@NonNull View v) {
                super(v);
                tvAvatar  = v.findViewById(R.id.tvConvAvatar);
                ivAvatar  = v.findViewById(R.id.ivConvAvatar);
                tvName    = v.findViewById(R.id.tvConvName);
                tvCarName = v.findViewById(R.id.tvConvCarName);
                tvLastMsg = v.findViewById(R.id.tvConvLastMsg);
            }
        }
    }
    // ══════════════════════════════════════════════════════════════════════════
    //  Xử lý chuyển Tab: Trò chuyện / Thông báo
    // ══════════════════════════════════════════════════════════════════════════
    private void selectTab(boolean chatSelected) {
        if (chatSelected == isChatTabActive) return;
        isChatTabActive = chatSelected;

        TransitionManager.beginDelayedTransition(
                frameMsgContent,
                new AutoTransition().setDuration(200));

        layoutChatTabContent.setVisibility(chatSelected ? View.VISIBLE : View.GONE);
        layoutNotificationTabContent.setVisibility(chatSelected ? View.GONE : View.VISIBLE);

        int activeColor   = Color.parseColor("#2F54D4");
        int inactiveColor = Color.WHITE;

        if (chatSelected) {
            contentTabChat.setBackgroundResource(R.drawable.bg_tab_active_pill);
            contentTabNotification.setBackground(null);
        } else {
            contentTabNotification.setBackgroundResource(R.drawable.bg_tab_active_pill);
            contentTabChat.setBackground(null);
        }

        tvTabChat.setTextColor(chatSelected ? activeColor : inactiveColor);
        tvTabNotification.setTextColor(!chatSelected ? activeColor : inactiveColor);
    }

}