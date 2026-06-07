package com.example.doanmb.ui.activity;import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.adapter.ChatAdapter;
import com.example.doanmb.model.Car;
import com.example.doanmb.model.ChatMessage;
import com.example.doanmb.util.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ListenerRegistration chatListener;
    private RecyclerView rvMessages;
    private ChatAdapter chatAdapter;

    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnAddImage, btnRemovePreview;
    private View layoutLoading, layoutImagePreview, rootLayout;
    private ImageView ivCar, ivPreview;
    private TextView tvPartnerName, tvCarName, tvCarPrice;
    private Button btnViewPost;

    private Uri pendingImageUri = null;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        showImagePreview(imageUri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        roomId = getIntent().getStringExtra("ROOM_ID");
        partnerId = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");
        carData = (Car) getIntent().getSerializableExtra("CAR_DATA");

        if (roomId == null || partnerId == null) {
            Toast.makeText(this, "Lỗi dữ liệu cuộc hội thoại", Toast.LENGTH_SHORT).show();
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
        rootLayout = findViewById(R.id.root_layout);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnBack = findViewById(R.id.btn_back);
        btnAddImage = findViewById(R.id.btn_add_image);
        layoutLoading = findViewById(R.id.layout_loading);

        layoutImagePreview = findViewById(R.id.layout_image_preview);
        ivPreview = findViewById(R.id.iv_preview);
        btnRemovePreview = findViewById(R.id.btn_remove_preview);

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
        btnSend.setOnClickListener(v -> handleSendMessage());
        btnAddImage.setOnClickListener(v -> openGallery());
        btnRemovePreview.setOnClickListener(v -> clearImagePreview());

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        // Xử lý Bàn phím: Tự động cuộn xuống khi bàn phím hiện ra (Fix lỗi che ô nhập liệu)
        rootLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) { // Bàn phím đang được mở
                scrollToBottom();
            }
        });

        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) scrollToBottom();
        });

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSendMessage();
                return true;
            }
            return false;
        });

        btnViewPost.setOnClickListener(v -> {
            if (carData != null && carData.getId() != null) {
                Intent intent = new Intent(this, CarDetailActivity.class);
                intent.putExtra("CAR_DATA", carData);
                intent.putExtra("CAR_ID", carData.getId());
                intent.putExtra("SELLER_ID", carData.getSellerId());
                intent.putExtra("CAR_TYPE", carData.getType());
                startActivity(intent);
            }
        });

        findViewById(R.id.btn_menu_more).setOnClickListener(this::showPopupMenu);
    }

    private void scrollToBottom() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1), 100);
        }
    }

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);
        rvMessages.setHasFixedSize(true);
    }

    private void listenForMessages() {
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("chat_rooms").document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    List<ChatMessage> newList = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            newList.add(msg);
                        }
                    }
                    chatAdapter.submitList(newList, this::scrollToBottom);
                    updateReadStatus();
                });
    }

    private void handleSendMessage() {
        if (isBlocked) return;
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() && pendingImageUri == null) return;

        if (pendingImageUri != null) {
            uploadAndSend(content, pendingImageUri);
        } else {
            performSendMessage(content, null);
        }
    }

    private void uploadAndSend(String content, Uri uri) {
        layoutLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);
        CloudinaryHelper.uploadImage(getApplicationContext(), uri, new CloudinaryHelper.OnUploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    performSendMessage(content, imageUrl);
                    clearImagePreview();
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    updateSendButtonState();
                    Toast.makeText(ChatDetailActivity.this, "Lỗi tải ảnh: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performSendMessage(String content, String imageUrl) {
        etMessage.setText("");
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId", currentUserId);
        msg.put("content", content);
        if (imageUrl != null) msg.put("imageUrl", imageUrl);
        msg.put("timestamp", FieldValue.serverTimestamp());
        msg.put("status", 0);

        db.collection("chat_rooms").document(roomId).collection("messages").add(msg)
                .addOnSuccessListener(ref -> {
                    String lastDisplay = imageUrl != null ? "[Hình ảnh]" : content;
                    db.collection("chat_rooms").document(roomId).update(
                            "lastMessage", lastDisplay,
                            "lastTimestamp", FieldValue.serverTimestamp()
                    );
                });
    }

    private void showImagePreview(Uri uri) {
        pendingImageUri = uri;
        layoutImagePreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).into(ivPreview);
        updateSendButtonState();
        scrollToBottom();
    }

    private void clearImagePreview() {
        pendingImageUri = null;
        layoutImagePreview.setVisibility(View.GONE);
        ivPreview.setImageDrawable(null);
        updateSendButtonState();
    }

    private void updateSendButtonState() {
        if (isBlocked) {
            btnSend.setEnabled(false);
            return;
        }
        boolean hasText = etMessage.getText().toString().trim().length() > 0;
        boolean hasImage = pendingImageUri != null;
        btnSend.setEnabled(hasText || hasImage);
    }

    private void openGallery() {
        if (isBlocked) return;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void updateReadStatus() {
        db.collection("chat_rooms").document(roomId).collection("messages")
                .whereEqualTo("senderId", partnerId).whereLessThan("status", 2)
                .get().addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snaps) {
                        batch.update(doc.getReference(), "status", 2);
                    }
                    batch.commit();
                });
    }

    private void checkBlockStatus() {
        db.collection("blocks").document(currentUserId + "_" + partnerId).addSnapshotListener((doc, e) -> {
            isBlocked = doc != null && doc.exists();
            if (isBlocked) {
                etMessage.setHint("Bạn đã chặn người này");
                etMessage.setEnabled(false);
                btnSend.setEnabled(false);
                btnAddImage.setEnabled(false);
            } else {
                etMessage.setHint("Nhập tin nhắn...");
                etMessage.setEnabled(true);
                btnAddImage.setEnabled(true);
                updateSendButtonState();
            }
        });
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

    private void blockUser() {
        Map<String, Object> block = new HashMap<>();
        block.put("blockerId", currentUserId);
        block.put("blockedId", partnerId);
        block.put("timestamp", FieldValue.serverTimestamp());
        db.collection("blocks").document(currentUserId + "_" + partnerId).set(block);
    }

    private void unblockUser() {
        db.collection("blocks").document(currentUserId + "_" + partnerId).delete();
    }

    private void showReportDialog() {
        String[] reasons = {"Lừa đảo", "Spam", "Khác"};
        new AlertDialog.Builder(this).setTitle("Báo cáo").setItems(reasons, (dialog, which) -> {
            Map<String, Object> report = new HashMap<>();
            report.put("reporterId", currentUserId);
            report.put("targetId", partnerId);
            report.put("reason", reasons[which]);
            report.put("timestamp", FieldValue.serverTimestamp());
            db.collection("reports").add(report);
            Toast.makeText(this, "Đã gửi báo cáo", Toast.LENGTH_SHORT).show();
        }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }
}