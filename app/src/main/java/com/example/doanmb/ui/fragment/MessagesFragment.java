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

    private LinearLayout layoutNotLoggedIn, layoutEmpty, layoutContent;
    private RecyclerView rvConversations, rvShortcuts;
    private EditText etSearch;
    private TextView tvGreeting;
    private ImageView imgAvatar;
    
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

        // Ánh xạ các ID từ XML (Đảm bảo ID khớp chính xác với fragment_messages.xml)
        layoutNotLoggedIn = view.findViewById(R.id.layout_msg_not_logged_in);
        layoutEmpty       = view.findViewById(R.id.layout_msg_empty);
        layoutContent     = view.findViewById(R.id.layout_msg_content);
        etSearch          = view.findViewById(R.id.et_search_chat);
        tvGreeting        = view.findViewById(R.id.tv_msg_greeting);
        imgAvatar         = view.findViewById(R.id.img_msg_avatar);
        
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
        String query = text.toLowerCase().trim();
        
        if (query.isEmpty()) {
            filteredList.addAll(convList);
        } else {
            for (Map<String, Object> item : convList) {
                String carName = String.valueOf(item.get("carName")).toLowerCase();
                String partnerName = item.containsKey("partnerName") ? String.valueOf(item.get("partnerName")).toLowerCase() : "";
                
                if (carName.contains(query) || partnerName.contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
        
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(filteredList.isEmpty() && !query.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (layoutNotLoggedIn != null) layoutNotLoggedIn.setVisibility(View.VISIBLE);
            if (layoutContent != null) layoutContent.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            return;
        }
        if (layoutNotLoggedIn != null) layoutNotLoggedIn.setVisibility(View.GONE);
        if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
        
        loadUserProfile(user.getUid());
        loadConversations(user.getUid());
    }

    private void loadUserProfile(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String name = doc.getString("name");
                String avatarUrl = doc.getString("avatarUrl");
                if (tvGreeting != null) tvGreeting.setText("Hi, " + (name != null ? name : "User"));
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
                        Map<String, Object> data = doc.getData();
                        data.put("roomId", doc.getId());
                        
                        String buyerId = (String) data.get("buyerId");
                        String sellerId = (String) data.get("sellerId");
                        String partnerId = uid.equals(buyerId) ? sellerId : buyerId;
                        
                        db.collection("users").document(partnerId).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists() && isAdded()) {
                                String name = userDoc.getString("name");
                                String avatar = userDoc.getString("avatarUrl");
                                data.put("partnerName", name);
                                data.put("partnerAvatar", avatar);
                                
                                if (!addedPartners.containsKey(partnerId) && shortcutList.size() < 10) {
                                    shortcutList.add(new HashMap<>(data));
                                    addedPartners.put(partnerId, true);
                                    shortcutAdapter.notifyDataSetChanged();
                                }
                                adapter.notifyDataSetChanged();
                                filter(etSearch.getText().toString());
                            }
                        });

                        convList.add(data);
                    }
                    adapter.notifyDataSetChanged();
                    filter(etSearch.getText().toString());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) listener.remove();
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, Object> item = list.get(position);
            String carName = (String) item.get("carName");
            String lastMsg = (String) item.get("lastMessage");
            String partnerName = (String) item.get("partnerName");
            String partnerAvatar = (String) item.get("partnerAvatar");

            h.tvCarName.setText(carName);
            h.tvLastMsg.setText(lastMsg == null || lastMsg.isEmpty() ? "Bắt đầu trò chuyện..." : lastMsg);

            if (partnerName != null) {
                h.tvName.setText(partnerName);
                String initial = String.valueOf(partnerName.charAt(0)).toUpperCase();
                h.tvAvatar.setText(initial);
                if (partnerAvatar != null && !partnerAvatar.isEmpty()) {
                    h.tvAvatar.setVisibility(View.GONE);
                    h.ivAvatar.setVisibility(View.VISIBLE);
                    Glide.with(h.ivAvatar.getContext()).load(partnerAvatar).transform(new CircleCrop()).into(h.ivAvatar);
                } else {
                    h.tvAvatar.setVisibility(View.VISIBLE);
                    h.ivAvatar.setVisibility(View.GONE);
                }
            } else {
                h.tvName.setText("Đang tải...");
            }

            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatDetailActivity.class);
                intent.putExtra("ROOM_ID", (String) item.get("roomId"));
                intent.putExtra("PARTNER_ID", currentUid.equals(item.get("buyerId")) ? (String)item.get("sellerId") : (String)item.get("buyerId"));
                intent.putExtra("PARTNER_NAME", partnerName);
                
                Car car = new Car(carName, (String)item.get("carPrice"), "", 0);
                car.setId((String)item.get("carId"));
                car.setImageUrl((String)item.get("carImage"));
                car.setSellerId((String)item.get("sellerId"));
                car.setType((String)item.get("carType"));
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
}
