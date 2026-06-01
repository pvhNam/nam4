package com.example.doanmb.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.adapter.ChatAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatDetailActivity extends AppCompatActivity {

    private String roomId, currentUserId, partnerId, partnerName;
    private Car carData;
    private boolean isBlocked = false;
    
    private FirebaseFirestore db;
    private RecyclerView rvMessages;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvPartnerName, tvCarName, tvCarPrice;
    private ImageView ivCar;
    private Button btnViewPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        roomId = getIntent().getStringExtra("ROOM_ID");
        partnerId = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");
        carData = (Car) getIntent().getSerializableExtra("CAR_DATA");

        if (roomId == null || partnerId == null) {
            Toast.makeText(this, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupChat();
        listenForMessages();
        updateReadStatus();
        checkBlockStatus();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        tvPartnerName = findViewById(R.id.tv_partner_name);
        ivCar = findViewById(R.id.iv_car);
        tvCarName = findViewById(R.id.tv_car_name);
        tvCarPrice = findViewById(R.id.tv_car_price);
        btnViewPost = findViewById(R.id.btn_view_post);

        tvPartnerName.setText(partnerName != null ? partnerName : "Người dùng");
        if (carData != null) {
            tvCarName.setText(carData.getName());
            tvCarPrice.setText(carData.getPrice());
            Glide.with(this).load(carData.getImageUrl()).placeholder(R.drawable.ic_buy_car).into(ivCar);
        }

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isBlocked) btnSend.setEnabled(s.toString().trim().length() > 0);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnViewPost.setOnClickListener(v -> {
            Intent intent = new Intent(this, CarDetailActivity.class);
            intent.putExtra("CAR_DATA", carData);
            startActivity(intent);
        });

        // Thiết lập menu 3 chấm (Option Menu)
        findViewById(R.id.btn_menu_more).setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        String[] options = isBlocked ? new String[]{"Bỏ chặn", "Báo cáo"} : new String[]{"Chặn người này", "Báo cáo"};
        new AlertDialog.Builder(this)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (isBlocked) unblockUser(); else blockUser();
                    } else {
                        showReportDialog();
                    }
                }).show();
    }

    private void checkBlockStatus() {
        db.collection("blocks").document(currentUserId + "_" + partnerId).addSnapshotListener((doc, e) -> {
            isBlocked = doc != null && doc.exists();
            if (isBlocked) {
                etMessage.setHint("Bạn đã chặn người này");
                etMessage.setEnabled(false);
                btnSend.setEnabled(false);
            } else {
                etMessage.setHint("Nhập tin nhắn...");
                etMessage.setEnabled(true);
            }
        });
    }

    private void blockUser() {
        Map<String, Object> block = new HashMap<>();
        block.put("blockerId", currentUserId);
        block.put("blockedId", partnerId);
        block.put("timestamp", FieldValue.serverTimestamp());
        db.collection("blocks").document(currentUserId + "_" + partnerId).set(block)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã chặn người dùng", Toast.LENGTH_SHORT).show());
    }

    private void unblockUser() {
        db.collection("blocks").document(currentUserId + "_" + partnerId).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã bỏ chặn", Toast.LENGTH_SHORT).show());
    }

    private void showReportDialog() {
        String[] reasons = {"Lừa đảo", "Spam", "Ngôn từ không phù hợp", "Khác"};
        new AlertDialog.Builder(this).setTitle("Báo cáo người dùng")
                .setItems(reasons, (dialog, which) -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("reporterId", currentUserId);
                    report.put("targetId", partnerId);
                    report.put("reason", reasons[which]);
                    report.put("roomId", roomId);
                    report.put("timestamp", FieldValue.serverTimestamp());
                    db.collection("reports").add(report);
                    Toast.makeText(this, "Cảm ơn bạn đã báo cáo", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter(messageList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(chatAdapter);
    }

    private void listenForMessages() {
        db.collection("chat_rooms").document(roomId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    messageList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) messageList.add(msg);
                    }
                    chatAdapter.notifyDataSetChanged();
                    rvMessages.scrollToPosition(messageList.size() - 1);
                    updateReadStatus();
                });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() || isBlocked) return;
        etMessage.setText("");
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("content", content);
        msg.put("timestamp", FieldValue.serverTimestamp());
        msg.put("status", 0);
        db.collection("chat_rooms").document(roomId).collection("messages").add(msg);
        db.collection("chat_rooms").document(roomId).update("lastMessage", content, "lastTimestamp", FieldValue.serverTimestamp());
    }

    private void updateReadStatus() {
        db.collection("chat_rooms").document(roomId).collection("messages")
                .whereEqualTo("senderId", partnerId).whereLessThan("status", 2)
                .get().addOnSuccessListener(snaps -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snaps) batch.update(doc.getReference(), "status", 2);
                    batch.commit();
                });
    }
}
