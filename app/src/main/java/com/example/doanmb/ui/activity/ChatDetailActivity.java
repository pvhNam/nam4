package com.example.doanmb.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.doanmb.R;
import com.example.doanmb.adapter.ChatAdapter;
import com.example.doanmb.adapter.MediaPickerAdapter;
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

    // Media picker panel
    private LinearLayout layoutMediaPicker;
    private ImageView tvPickerClose;
    private RecyclerView rvMediaPicker;
    private Button btnPickerSend;
    private MediaPickerAdapter mediaPickerAdapter;
    private boolean isPickerOpen = false;

    // Tab switching trong picker: IMAGE hoặc VIDEO
    private LinearLayout btnPickerImage, btnPickerVideo;
    private ImageView ivTabImage, ivTabVideo;
    private View indicatorImage, indicatorVideo;
    private boolean isVideoMode = false; // false = ảnh, true = video

    // Media đã chọn để gửi (ảnh + video cộng gộp)
    private final List<MediaPickerAdapter.MediaItem> pendingMedia = new ArrayList<>();

    // Permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = false;
                for (Boolean v : result.values()) if (v) { granted = true; break; }
                if (granted) openMediaPicker(isVideoMode);
                else Toast.makeText(this, "Cần quyền truy cập thư viện!", Toast.LENGTH_SHORT).show();
            });

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
        setupMediaPicker();
        listenForMessages();
        updateReadStatus();
        checkBlockStatus();
    }

    // ── Init Views ───────────────────────────────────────────────────────────

    private void initViews() {
        rootLayout        = findViewById(R.id.root_layout);
        rvMessages        = findViewById(R.id.rv_messages);
        etMessage         = findViewById(R.id.et_message);
        btnSend           = findViewById(R.id.btn_send);
        btnBack           = findViewById(R.id.btn_back);
        btnAddMedia       = findViewById(R.id.btn_add_image);
        layoutLoading     = findViewById(R.id.layout_loading);
        tvPartnerName     = findViewById(R.id.tv_partner_name);
        ivCar             = findViewById(R.id.iv_car);
        tvCarName         = findViewById(R.id.tv_car_name);
        tvCarPrice        = findViewById(R.id.tv_car_price);
        btnViewPost       = findViewById(R.id.btn_view_post);
        layoutMediaPicker = findViewById(R.id.layout_media_picker);
        tvPickerClose     = findViewById(R.id.tv_picker_close);
        rvMediaPicker     = findViewById(R.id.rv_media_picker);
        btnPickerSend     = findViewById(R.id.btn_picker_send);

        // Tab icons
        btnPickerImage  = findViewById(R.id.btn_picker_image);
        btnPickerVideo  = findViewById(R.id.btn_picker_video);
        ivTabImage      = findViewById(R.id.iv_tab_image);
        ivTabVideo      = findViewById(R.id.iv_tab_video);
        indicatorImage  = findViewById(R.id.indicator_image);
        indicatorVideo  = findViewById(R.id.indicator_video);

        tvPartnerName.setText(partnerName != null ? partnerName : "Người dùng");
        if (carData != null) {
            tvCarName.setText(carData.getName());
            tvCarPrice.setText(carData.getPrice());
            Glide.with(this).load(carData.getImageUrl())
                    .placeholder(R.drawable.ic_buy_car).into(ivCar);
        }

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSendMessage());

        // Nút thêm media ở thanh nhập: mở picker ở tab ảnh (mặc định)
        btnAddMedia.setOnClickListener(v -> {
            if (isBlocked) return;
            if (isPickerOpen) {
                closeMediaPicker();
            } else {
                isVideoMode = false;
                checkPermissionAndOpen();
            }
        });

        // Tab ảnh
        btnPickerImage.setOnClickListener(v -> {
            if (isVideoMode) {
                isVideoMode = false;
                applyTabStyle();
                reloadPickerContent();
            }
        });

        // Tab video
        btnPickerVideo.setOnClickListener(v -> {
            if (!isVideoMode) {
                isVideoMode = true;
                applyTabStyle();
                reloadPickerContent();
            }
        });

        // Nút đóng (X): reset tất cả selection rồi đóng picker
        if (tvPickerClose != null) {
            tvPickerClose.setOnClickListener(v -> {
                pendingMedia.clear();
                if (mediaPickerAdapter != null) mediaPickerAdapter.clearSelection();
                updatePickerSendButton();
                updateSendButtonState();
                closeMediaPicker();
            });
        }

        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) { updateSendButtonState(); }
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        rootLayout.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) scrollToBottom();
        });

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

        findViewById(R.id.btn_menu_more).setOnClickListener(this::showPopupMenu);
    }

    // ── Áp dụng style cho 2 tab icon ─────────────────────────────────────────

    private void applyTabStyle() {
        if (isVideoMode) {
            // Video tab active
            ivTabImage.setAlpha(0.45f);
            indicatorImage.setVisibility(View.INVISIBLE);
            ivTabVideo.setAlpha(1.0f);
            indicatorVideo.setVisibility(View.VISIBLE);
        } else {
            // Image tab active
            ivTabImage.setAlpha(1.0f);
            indicatorImage.setVisibility(View.VISIBLE);
            ivTabVideo.setAlpha(0.45f);
            indicatorVideo.setVisibility(View.INVISIBLE);
        }
    }

    // ── Reload nội dung picker theo tab hiện tại ─────────────────────────────

    private void reloadPickerContent() {
        if (mediaPickerAdapter == null) return;
        new Thread(() -> {
            mediaPickerAdapter.loadFromDevice(this, isVideoMode);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    mediaPickerAdapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    // ── Media Picker ─────────────────────────────────────────────────────────

    private void setupMediaPicker() {
        if (rvMediaPicker == null) return;
        mediaPickerAdapter = new MediaPickerAdapter();
        rvMediaPicker.setLayoutManager(new GridLayoutManager(this, 3));
        rvMediaPicker.setAdapter(mediaPickerAdapter);

        mediaPickerAdapter.setOnMediaSelectedListener(selected -> {
            // Cập nhật pendingMedia: giữ item từ tab kia, cộng thêm item từ tab hiện tại
            // Xoá các item cùng loại (ảnh hoặc video) với tab hiện tại khỏi pending
            List<MediaPickerAdapter.MediaItem> toKeep = new ArrayList<>();
            for (MediaPickerAdapter.MediaItem item : pendingMedia) {
                if (item.isVideo != isVideoMode) {
                    toKeep.add(item); // giữ lại item từ tab kia
                }
            }
            pendingMedia.clear();
            pendingMedia.addAll(toKeep);
            pendingMedia.addAll(selected);

            updatePickerSendButton();
            updateSendButtonState();
        });

        // Bấm nút Gửi N trong picker → gửi và đóng picker
        if (btnPickerSend != null) {
            btnPickerSend.setOnClickListener(v -> {
                if (!pendingMedia.isEmpty()) {
                    handleSendMessage();
                }
                closeMediaPicker();
            });
        }
    }

    private void updatePickerSendButton() {
        if (btnPickerSend == null) return;
        int total = pendingMedia.size();
        if (total > 0) {
            btnPickerSend.setVisibility(View.VISIBLE);
            btnPickerSend.setText("Gửi " + total);
        } else {
            btnPickerSend.setVisibility(View.GONE);
        }
    }

    private void checkPermissionAndOpen() {
        boolean hasImage, hasVideo;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            hasImage = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            hasVideo = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            if (!hasImage || !hasVideo) {
                permissionLauncher.launch(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO});
                return;
            }
        } else {
            boolean hasStorage = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!hasStorage) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                return;
            }
        }
        openMediaPicker(isVideoMode);
    }

    private void openMediaPicker(boolean videoMode) {
        if (layoutMediaPicker == null || mediaPickerAdapter == null) return;
        isPickerOpen = true;
        isVideoMode = videoMode;
        applyTabStyle();
        layoutMediaPicker.setVisibility(View.VISIBLE);
        new Thread(() -> {
            mediaPickerAdapter.loadFromDevice(this, isVideoMode);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    mediaPickerAdapter.notifyDataSetChanged();
                    scrollToBottom();
                }
            });
        }).start();
    }

    private void closeMediaPicker() {
        isPickerOpen = false;
        if (layoutMediaPicker != null) {
            layoutMediaPicker.setVisibility(View.GONE);
        }
    }

    // ── Setup Chat ───────────────────────────────────────────────────────────

    private void setupChat() {
        chatAdapter = new ChatAdapter();
        chatAdapter.setOnMediaClickListener(msg -> openFullscreenMedia(msg));
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(chatAdapter);
        rvMessages.setHasFixedSize(true);
    }

    private void openFullscreenMedia(ChatMessage clickedMsg) {
        List<ChatMessage> allMessages = new ArrayList<>();
        for (int i = 0; i < chatAdapter.getItemCount(); i++) {
            allMessages.add(chatAdapter.getCurrentList().get(i));
        }

        ArrayList<String>  urls     = new ArrayList<>();
        ArrayList<Boolean> isVideos = new ArrayList<>();
        int startPos = 0;

        for (ChatMessage msg : allMessages) {
            if (msg.isVideo() && msg.getVideoUrl() != null && !msg.getVideoUrl().isEmpty()) {
                if (msg == clickedMsg || (clickedMsg.getVideoUrl() != null
                        && clickedMsg.getVideoUrl().equals(msg.getVideoUrl()))) {
                    startPos = urls.size();
                }
                urls.add(msg.getVideoUrl());
                isVideos.add(true);
            } else if (!msg.isVideo() && msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                if (msg == clickedMsg || (clickedMsg.getImageUrl() != null
                        && clickedMsg.getImageUrl().equals(msg.getImageUrl()))) {
                    startPos = urls.size();
                }
                urls.add(msg.getImageUrl());
                isVideos.add(false);
            }
        }

        if (urls.isEmpty()) return;

        Intent intent = new Intent(this, FullscreenMediaActivity.class);
        intent.putStringArrayListExtra(FullscreenMediaActivity.EXTRA_URLS, urls);
        intent.putExtra(FullscreenMediaActivity.EXTRA_IS_VIDEOS, isVideos);
        intent.putExtra(FullscreenMediaActivity.EXTRA_START_POS, startPos);
        startActivity(intent);
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
            pendingMedia.clear();
            if (mediaPickerAdapter != null) mediaPickerAdapter.clearSelection();
            updatePickerSendButton();
            closeMediaPicker();
        } else {
            performSendMessage(content, null, null);
            etMessage.setText("");
        }
    }

    private void sendMediaSequentially(List<MediaPickerAdapter.MediaItem> items, String textContent) {
        layoutLoading.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        final int total = items.size();
        final int[] doneCount = {0};

        for (int i = 0; i < total; i++) {
            final int index = i;
            MediaPickerAdapter.MediaItem item = items.get(index);
            final String msgText = (index == 0) ? textContent : "";

            if (item.isVideo) {
                CloudinaryHelper.uploadVideo(getApplicationContext(), item.uri,
                        new CloudinaryHelper.OnUploadCallback() {
                            @Override public void onSuccess(String url) {
                                runOnUiThread(() -> {
                                    performSendMessage(msgText, null, url);
                                    finishOneUpload(doneCount, total);
                                });
                            }
                            @Override public void onFailure(String error) {
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
                            @Override public void onSuccess(String url) {
                                runOnUiThread(() -> {
                                    performSendMessage(msgText, url, null);
                                    finishOneUpload(doneCount, total);
                                });
                            }
                            @Override public void onFailure(String error) {
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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateSendButtonState() {
        if (isBlocked) { btnSend.setEnabled(false); return; }
        btnSend.setEnabled(
                etMessage.getText().toString().trim().length() > 0
                        || !pendingMedia.isEmpty());
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
    public void onBackPressed() {
        if (isPickerOpen) {
            closeMediaPicker();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) chatListener.remove();
    }
}