package com.example.doanmb.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    // Views
    private RecyclerView rvMessages;
    private ChatAdapter chatAdapter;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnAddMedia;
    private View layoutLoading, rootLayout;
    private ImageView ivCar;
    private TextView tvPartnerName, tvCarName, tvCarPrice;
    private Button btnViewPost;

    // Media preview
    private final List<MediaItem> pendingMedia = new ArrayList<>();
    private HorizontalScrollView scrollMediaPreview;
    private LinearLayout layoutMediaPreviews;

    // ── Launchers ────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> pickImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++)
                            addMediaToPreview(data.getClipData().getItemAt(i).getUri(), false);
                    } else if (data.getData() != null) {
                        addMediaToPreview(data.getData(), false);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> pickVideoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    addMediaToPreview(result.getData().getData(), true);
                }
            });

    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        roomId      = getIntent().getStringExtra("ROOM_ID");
        partnerId   = getIntent().getStringExtra("PARTNER_ID");
        partnerName = getIntent().getStringExtra("PARTNER_NAME");
        carData     = (Car) getIntent().getSerializableExtra("CAR_DATA");

        if (roomId == null || partnerId == null) { finish(); return; }

        initViews();
        setupChat();
        listenForMessages();
        updateReadStatus();
        checkBlockStatus();
    }

    // ── Init Views ───────────────────────────────────────────────────────────

    private void initViews() {
        rootLayout          = findViewById(R.id.root_layout);
        rvMessages          = findViewById(R.id.rv_messages);
        etMessage           = findViewById(R.id.et_message);
        btnSend             = findViewById(R.id.btn_send);
        btnBack             = findViewById(R.id.btn_back);
        btnAddMedia         = findViewById(R.id.btn_add_image);
        layoutLoading       = findViewById(R.id.layout_loading);
        tvPartnerName       = findViewById(R.id.tv_partner_name);
        ivCar               = findViewById(R.id.iv_car);
        tvCarName           = findViewById(R.id.tv_car_name);
        tvCarPrice          = findViewById(R.id.tv_car_price);
        btnViewPost         = findViewById(R.id.btn_view_post);
        scrollMediaPreview  = findViewById(R.id.scroll_image_preview);
        layoutMediaPreviews = findViewById(R.id.layout_image_previews);

        tvPartnerName.setText(partnerName != null ? partnerName : "Người dùng");
        if (carData != null) {
            tvCarName.setText(carData.getName());
            tvCarPrice.setText(carData.getPrice());
            Glide.with(this).load(carData.getImageUrl())
                    .placeholder(R.drawable.ic_buy_car).into(ivCar);
        }

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSendMessage());
        btnAddMedia.setOnClickListener(v -> {
            if (isBlocked) return;
            showMediaPickerDialog();
        });

        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) { updateSendButtonState(); }
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        rootLayout.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) scrollToBottom();
        });
        etMessage.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) scrollToBottom(); });
        findViewById(R.id.btn_menu_more).setOnClickListener(this::showPopupMenu);

        if (btnViewPost != null) {
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
        }
    }

    // ── Dialog chọn Ảnh / Video ──────────────────────────────────────────────

    private void showMediaPickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chọn media")
                .setItems(new String[]{"🖼️  Ảnh (nhiều)", "🎬  Video"}, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        pickImagesLauncher.launch(intent);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("video/*");
                        pickVideoLauncher.launch(intent);
                    }
                })
                .show();
    }

    // ── Setup Chat ───────────────────────────────────────────────────────────

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnMediaClickListener(this::openMedia);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(chatAdapter);
        rvMessages.setHasFixedSize(true);
    }

    /** Mở ảnh/video của tin nhắn bằng trình xem ngoài (video phát được). */
    private void openMedia(ChatMessage msg) {
        String url = msg.isVideo() ? msg.getVideoUrl() : msg.getImageUrl();
        if (url == null || url.isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), msg.isVideo() ? "video/*" : "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ex) {
                Toast.makeText(this, "Không mở được nội dung", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void listenForMessages() {
        if (chatListener != null) chatListener.remove();
        chatListener = db.collection("chat_rooms").document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    List<ChatMessage> list = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) { msg.setMessageId(doc.getId()); list.add(msg); }
                    }
                    chatAdapter.submitList(list, this::scrollToBottom);
                    updateReadStatus();
                });
    }

    // ── Gửi tin nhắn ────────────────────────────────────────────────────────

    private void handleSendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() && pendingMedia.isEmpty()) return;

        if (!pendingMedia.isEmpty()) {
            sendMediaSequentially(new ArrayList<>(pendingMedia), content);
            clearAllPreviews();
        } else {
            performSendMessage(content, null, null);
            etMessage.setText("");
        }
    }

    private void sendMediaSequentially(List<MediaItem> items, String textContent) {
        layoutLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        final int total = items.size();
        final int[] doneCount = {0};

        for (int i = 0; i < total; i++) {
            final int index = i;
            MediaItem item  = items.get(index);
            final String msgText = (index == 0) ? textContent : "";

            if (item.isVideo) {
                CloudinaryHelper.uploadVideo(getApplicationContext(), item.uri,
                        new CloudinaryHelper.OnUploadCallback() {
                            @Override
                            public void onSuccess(String videoUrl) {
                                runOnUiThread(() -> {
                                    performSendMessage(msgText, null, videoUrl);
                                    finishOneUpload(doneCount, total);
                                });
                            }
                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ChatDetailActivity.this,
                                            "Lỗi video: " + error, Toast.LENGTH_SHORT).show();
                                    finishOneUpload(doneCount, total);
                                });
                            }
                        });
            } else {
                CloudinaryHelper.uploadImage(getApplicationContext(), item.uri,
                        new CloudinaryHelper.OnUploadCallback() {
                            @Override
                            public void onSuccess(String imageUrl) {
                                runOnUiThread(() -> {
                                    performSendMessage(msgText, imageUrl, null);
                                    finishOneUpload(doneCount, total);
                                });
                            }
                            @Override
                            public void onFailure(String error) {
                                runOnUiThread(() -> {
                                    Toast.makeText(ChatDetailActivity.this,
                                            "Lỗi ảnh: " + error, Toast.LENGTH_SHORT).show();
                                    finishOneUpload(doneCount, total);
                                });
                            }
                        });
            }
        }
    }

    private void finishOneUpload(int[] doneCount, int total) {
        doneCount[0]++;
        if (doneCount[0] >= total) {
            layoutLoading.setVisibility(View.GONE);
            etMessage.setText("");
            updateSendButtonState();
        }
    }

    private void performSendMessage(String content, String imageUrl, String videoUrl) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("senderId",  currentUserId);
        msg.put("content",   content != null ? content : "");
        msg.put("timestamp", FieldValue.serverTimestamp());
        msg.put("status",    0);

        if (videoUrl != null && !videoUrl.isEmpty()) {
            msg.put("videoUrl",     videoUrl);
            msg.put("thumbnailUrl", CloudinaryHelper.getVideoThumbnailUrl(videoUrl));
            msg.put("messageType",  ChatMessage.TYPE_VIDEO);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            msg.put("imageUrl",    imageUrl);
            msg.put("messageType", ChatMessage.TYPE_IMAGE);
        } else {
            msg.put("messageType", ChatMessage.TYPE_TEXT);
        }

        db.collection("chat_rooms").document(roomId)
                .collection("messages").add(msg)
                .addOnSuccessListener(ref -> {
                    String last;
                    if (videoUrl != null)      last = "[Video]";
                    else if (imageUrl != null) last = "[Hình ảnh]";
                    else                       last = content;
                    db.collection("chat_rooms").document(roomId)
                            .update("lastMessage", last,
                                    "lastTimestamp", FieldValue.serverTimestamp());
                });
    }

    // ── Media Preview ────────────────────────────────────────────────────────

    private void addMediaToPreview(Uri uri, boolean isVideo) {
        MediaItem item = new MediaItem(uri, isVideo);
        pendingMedia.add(item);

        View thumb = LayoutInflater.from(this)
                .inflate(R.layout.item_image_preview_thumb, layoutMediaPreviews, false);

        ImageView ivThumb    = thumb.findViewById(R.id.iv_thumb);
        ImageView btnRemove  = thumb.findViewById(R.id.btn_remove_thumb);
        View      overlay    = thumb.findViewById(R.id.iv_play_overlay);
        ImageView ivPlayIcon = thumb.findViewById(R.id.iv_play_icon);

        Glide.with(this).load(uri).centerCrop().into(ivThumb);

        if (isVideo) {
            if (overlay   != null) overlay.setVisibility(View.VISIBLE);
            if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.VISIBLE);
        } else {
            if (overlay   != null) overlay.setVisibility(View.GONE);
            if (ivPlayIcon != null) ivPlayIcon.setVisibility(View.GONE);
        }

        btnRemove.setOnClickListener(v -> {
            pendingMedia.remove(item);
            layoutMediaPreviews.removeView(thumb);
            if (pendingMedia.isEmpty()) scrollMediaPreview.setVisibility(View.GONE);
            updateSendButtonState();
        });

        layoutMediaPreviews.addView(thumb);
        scrollMediaPreview.setVisibility(View.VISIBLE);
        updateSendButtonState();
        scrollToBottom();
    }

    private void clearAllPreviews() {
        pendingMedia.clear();
        layoutMediaPreviews.removeAllViews();
        scrollMediaPreview.setVisibility(View.GONE);
        updateSendButtonState();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateSendButtonState() {
        if (isBlocked) { btnSend.setEnabled(false); return; }
        btnSend.setEnabled(
                etMessage.getText().toString().trim().length() > 0
                        || !pendingMedia.isEmpty()
        );
    }

    private void scrollToBottom() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            rvMessages.postDelayed(
                    () -> rvMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1), 100);
        }
    }

    private void updateReadStatus() {
        db.collection("chat_rooms").document(roomId)
                .collection("messages")
                .whereEqualTo("senderId", partnerId)
                .whereLessThan("status", 2)
                .get().addOnSuccessListener(snaps -> {
                    if (snaps.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snaps)
                        batch.update(doc.getReference(), "status", 2);
                    batch.commit();
                });
    }

    private void checkBlockStatus() {
        db.collection("blocks")
                .document(currentUserId + "_" + partnerId)
                .addSnapshotListener((doc, e) -> {
                    isBlocked = doc != null && doc.exists();
                    etMessage.setHint(isBlocked ? "Bạn đã chặn người này" : "Nhập tin nhắn...");
                    etMessage.setEnabled(!isBlocked);
                    btnSend.setEnabled(!isBlocked);
                    btnAddMedia.setEnabled(!isBlocked);
                });
    }

    private void showPopupMenu(View v) {
        String[] options = isBlocked
                ? new String[]{"Bỏ chặn", "Báo cáo"}
                : new String[]{"Chặn người này", "Báo cáo"};
        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (isBlocked) {
                    db.collection("blocks").document(currentUserId + "_" + partnerId).delete();
                } else {
                    Map<String, Object> b = new HashMap<>();
                    b.put("blockerId", currentUserId);
                    b.put("blockedId", partnerId);
                    b.put("timestamp", FieldValue.serverTimestamp());
                    db.collection("blocks").document(currentUserId + "_" + partnerId).set(b);
                }
            } else {
                showReportDialog();
            }
        }).show();
    }

    private void showReportDialog() {
        String[] reasons = {"Lừa đảo", "Spam", "Khác"};
        new AlertDialog.Builder(this).setTitle("Báo cáo")
                .setItems(reasons, (dialog, which) -> {
                    Map<String, Object> report = new HashMap<>();
                    report.put("reporterId", currentUserId);
                    report.put("targetId",   partnerId);
                    report.put("reason",     reasons[which]);
                    report.put("timestamp",  FieldValue.serverTimestamp());
                    db.collection("reports").add(report);
                    Toast.makeText(this, "Đã gửi báo cáo", Toast.LENGTH_SHORT).show();
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }

    // ── Inner class ──────────────────────────────────────────────────────────

    private static class MediaItem {
        final Uri     uri;
        final boolean isVideo;
        MediaItem(Uri uri, boolean isVideo) {
            this.uri     = uri;
            this.isVideo = isVideo;
        }
    }
}