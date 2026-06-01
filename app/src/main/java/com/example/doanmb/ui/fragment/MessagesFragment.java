package com.example.doanmb.ui.fragment;

import android.content.Intent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {

    private LinearLayout layoutNotLoggedIn, layoutEmpty;
    private RecyclerView rvConversations, rvShortcuts;
    private EditText etSearch;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private ConversationAdapter adapter;
    private ShortcutAdapter shortcutAdapter;
    
    private final List<Map<String, Object>> convList = new ArrayList<>();
    private final List<Map<String, Object>> filteredList = new ArrayList<>();
    private final List<Map<String, Object>> shortcutList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        db = FirebaseFirestore.getInstance();

        layoutNotLoggedIn = view.findViewById(R.id.layout_msg_not_logged_in);
        layoutEmpty       = view.findViewById(R.id.layout_msg_empty);
        etSearch          = view.findViewById(R.id.et_search_chat);
        
        rvConversations   = view.findViewById(R.id.rv_conversations);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConversationAdapter(filteredList, db);
        rvConversations.setAdapter(adapter);

        rvShortcuts = view.findViewById(R.id.rv_shortcuts);
        rvShortcuts.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        shortcutAdapter = new ShortcutAdapter(shortcutList);
        rvShortcuts.setAdapter(shortcutAdapter);

        setupSearch();

        return view;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(convList);
        } else {
            String query = text.toLowerCase().trim();
            for (Map<String, Object> item : convList) {
                String carName = ((String) item.get("carName")).toLowerCase();
                // Note: Partner name is fetched asynchronously in adapter, 
                // for real search we might need to store partnerName in roomData or fetch beforehand.
                if (carName.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            layoutNotLoggedIn.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            rvConversations.setVisibility(View.GONE);
            rvShortcuts.setVisibility(View.GONE);
            return;
        }
        layoutNotLoggedIn.setVisibility(View.GONE);
        loadConversations(user.getUid());
    }

    private void loadConversations(String uid) {
        if (listener != null) listener.remove();

        listener = db.collection("chat_rooms")
                .whereArrayContains("participants", uid)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    if (!isAdded()) return;

                    convList.clear();
                    shortcutList.clear();
                    Map<String, Boolean> addedPartners = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Map<String, Object> data = doc.getData();
                        data.put("roomId", doc.getId());
                        convList.add(data);

                        // Xử lý Shortcut (Những người nhắn tin gần đây)
                        String buyerId = (String) data.get("buyerId");
                        String sellerId = (String) data.get("sellerId");
                        String partnerId = uid.equals(buyerId) ? sellerId : buyerId;

                        if (!addedPartners.containsKey(partnerId) && shortcutList.size() < 10) {
                            Map<String, Object> shortcut = new HashMap<>();
                            shortcut.put("partnerId", partnerId);
                            shortcut.put("roomId", doc.getId());
                            shortcut.put("carId", data.get("carId"));
                            shortcut.put("carName", data.get("carName"));
                            shortcut.put("carImage", data.get("carImage"));
                            
                            // Fetch name for shortcut
                            db.collection("users").document(partnerId).get().addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    shortcut.put("partnerName", userDoc.getString("name"));
                                    shortcut.put("partnerAvatar", userDoc.getString("avatarUrl"));
                                    shortcutAdapter.notifyDataSetChanged();
                                }
                            });
                            
                            shortcutList.add(shortcut);
                            addedPartners.put(partnerId, true);
                        }
                    }

                    filter(etSearch.getText().toString());
                    shortcutAdapter.notifyDataSetChanged();
                    
                    boolean isEmpty = convList.isEmpty();
                    rvConversations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    rvShortcuts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                    layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) { listener.remove(); listener = null; }
    }

    static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> {
        private final List<Map<String, Object>> list;
        private final FirebaseFirestore db;
        private final String currentUid;

        ConversationAdapter(List<Map<String, Object>> list, FirebaseFirestore db) {
            this.list = list;
            this.db   = db;
            this.currentUid = FirebaseAuth.getInstance().getCurrentUser() != null 
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
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
            String roomId = getStr(item, "roomId");
            String carName = getStr(item, "carName");
            String carPrice = getStr(item, "carPrice");
            String lastMsg = getStr(item, "lastMessage");
            String buyerId = getStr(item, "buyerId");
            String sellerId = getStr(item, "sellerId");
            String carType = getStr(item, "carType");
            String carId = getStr(item, "carId");
            String carImage = getStr(item, "carImage");

            String partnerId = currentUid.equals(buyerId) ? sellerId : buyerId;

            h.tvCarName.setText(carName);
            h.tvLastMsg.setText(lastMsg.isEmpty() ? "Bắt đầu cuộc trò chuyện..." : lastMsg);
            h.tvStatus.setVisibility(View.GONE);

            h.tvName.setText("Đang tải...");

            db.collection("users").document(partnerId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            String avatarUrl = doc.getString("avatarUrl");
                            h.tvName.setText(name != null ? name : "Người dùng");
                            String displayName = name != null ? name : "U";
                            String initial = String.valueOf(displayName.charAt(0)).toUpperCase();
                            h.tvAvatar.setText(initial);
                            h.tvAvatar.setBackgroundColor(getAvatarColor(displayName));
                            
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                h.tvAvatar.setVisibility(View.GONE);
                                h.ivAvatar.setVisibility(View.VISIBLE);
                                Glide.with(h.ivAvatar.getContext())
                                        .load(avatarUrl)
                                        .transform(new CircleCrop())
                                        .into(h.ivAvatar);
                            } else {
                                h.tvAvatar.setVisibility(View.VISIBLE);
                                h.ivAvatar.setVisibility(View.GONE);
                            }
                        }
                    });

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatDetailActivity.class);
                intent.putExtra("ROOM_ID", roomId);
                intent.putExtra("PARTNER_ID", partnerId);
                intent.putExtra("PARTNER_NAME", h.tvName.getText().toString());
                
                Car car = new Car(carName, carPrice, "", 0);
                car.setId(carId);
                car.setSellerId(sellerId);
                car.setImageUrl(carImage);
                car.setType(carType);
                intent.putExtra("CAR_DATA", car);
                
                v.getContext().startActivity(intent);
            });
        }

        private String getStr(Map<String, Object> map, String key) {
            Object val = map.get(key);
            return (val instanceof String) ? (String) val : "";
        }

        private int getAvatarColor(String name) {
            int[] colors = {0xFF1976D2, 0xFF388E3C, 0xFFF57C00, 0xFF7B1FA2, 0xFFC62828, 0xFF00838F};
            return colors[Math.abs(name.hashCode()) % colors.length];
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
