package com.example.doanmb.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Locale;
import java.util.Map;

public class ChatDetailActivity extends AppCompatActivity {

    private String roomId, currentUserId, partnerId, partnerName;
    private Car carData;

    // ── Trạng thái chặn (2 chiều) ────────────────────────────────────────────
    private boolean iBlockedPartner  = false;   // tôi chặn đối phương
    private boolean partnerBlockedMe = false;   // đối phương chặn tôi

    private FirebaseFirestore db;
    private ListenerRegistration chatListener;
    private ListenerRegistration blockListenerMine, blockListenerPartner;

    // ── Views cơ bản ──────────────────────────────────────────────────────────
    private RecyclerView rvMessages;
    private ChatAdapter  chatAdapter;
    private EditText     etMessage;
    private ImageButton  btnSend, btnBack, btnAddMedia;
    private View         layoutLoading, rootLayout;
    private ImageView    ivCar;
    private TextView     tvPartnerName, tvCarName, tvCarPrice;
    private TextView     tvBlockedBanner;
    private Button       btnViewPost;

    // ── Search views ──────────────────────────────────────────────────────────
    private ImageButton  btnSearchToggle, btnSearchClose, btnSearchPrev, btnSearchNext;
    private EditText     etSearchMessages;
    private LinearLayout layoutSearchBar, layoutSearchNav;
    private TextView     tvSearchResultInfo;

    // ── Search state ──────────────────────────────────────────────────────────
    private final List<ChatMessage> allMessages      = new ArrayList<>();
    private final List<Integer>     searchPositions  = new ArrayList<>();
    private int currentSearchIdx = -1;

    // ── Media picker ──────────────────────────────────────────────────────────
    private LinearLayout layoutMediaPicker;
    private ImageView    tvPickerClose;
    private RecyclerView rvMediaPicker;
    private Button       btnPickerSend;
    private MediaPickerAdapter mediaPickerAdapter;
    private boolean isPickerOpen = false;

    private LinearLayout btnPickerImage, btnPickerVideo;
    private ImageView    ivTabImage, ivTabVideo;
    private View         indicatorImage, indicatorVideo;
    private boolean      isVideoMode = false;

    private final List<MediaPickerAdapter.MediaItem> pendingMedia = new ArrayList<>();

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = false;
                for (Boolean v : result.values()) if (v) { granted = true; break; }
                if (granted) openMediaPicker(isVideoMode);
                else Toast.makeText(this, "Cần quyền truy cập thư viện!", Toast.LENGTH_SHORT).show();
            });

    // ══════════════════════════════════════════════════════════════════════════
    //  onCreate
    // ══════════════════════════════════════════════════════════════════════════
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
        setupSearch();
        listenForMessages();
        updateReadStatus();
        listenForBlockStatus(); // thay checkBlockStatus cũ
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Init Views
    // ══════════════════════════════════════════════════════════════════════════
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
        tvBlockedBanner   = findViewById(R.id.tv_blocked_banner);
        layoutMediaPicker = findViewById(R.id.layout_media_picker);
        tvPickerClose     = findViewById(R.id.tv_picker_close);
        rvMediaPicker     = findViewById(R.id.rv_media_picker);
        btnPickerSend     = findViewById(R.id.btn_picker_send);

        // Search
        btnSearchToggle   = findViewById(R.id.btn_search_toggle);
        btnSearchClose    = findViewById(R.id.btn_search_close);
        btnSearchPrev     = findViewById(R.id.btn_search_prev);
        btnSearchNext     = findViewById(R.id.btn_search_next);
        etSearchMessages  = findViewById(R.id.et_search_messages);
        layoutSearchBar   = findViewById(R.id.layout_search_bar);
        layoutSearchNav   = findViewById(R.id.layout_search_nav);
        tvSearchResultInfo = findViewById(R.id.tv_search_result_info);

        // Picker tabs
        btnPickerImage = findViewById(R.id.btn_picker_image);
        btnPickerVideo = findViewById(R.id.btn_picker_video);
        ivTabImage     = findViewById(R.id.iv_tab_image);
        ivTabVideo     = findViewById(R.id.iv_tab_video);
        indicatorImage = findViewById(R.id.indicator_image);
        indicatorVideo = findViewById(R.id.indicator_video);

        // ── Gán dữ liệu cơ bản ────────────────────────────────────────────
        tvPartnerName.setText(partnerName != null ? partnerName : "Người dùng");
        if (carData != null) {
            tvCarName.setText(carData.getName());
            tvCarPrice.setText(carData.getPrice());
            Glide.with(this).load(carData.getImageUrl())
                    .placeholder(R.drawable.ic_buy_car).into(ivCar);
        }

        // ── Listeners ─────────────────────────────────────────────────────
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> handleSendMessage());

        btnAddMedia.setOnClickListener(v -> {
            if (isAnyoneBlocked()) return;
            if (isPickerOpen) closeMediaPicker();
            else { isVideoMode = false; checkPermissionAndOpen(); }
        });

        btnPickerImage.setOnClickListener(v -> {
            if (isVideoMode) { isVideoMode = false; applyTabStyle(); reloadPickerContent(); }
        });
        btnPickerVideo.setOnClickListener(v -> {
            if (!isVideoMode) { isVideoMode = true; applyTabStyle(); reloadPickerContent(); }
        });

        if (tvPickerClose != null) {
            tvPickerClose.setOnClickListener(v -> {
                pendingMedia.clear();
                if (mediaPickerAdapter != null) mediaPickerAdapter.clearSelection();
                updatePickerSendButton();
                updateSendButtonState();
                closeMediaPicker();
            });
        }

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) { updateSendButtonState(); }
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
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

    // ══════════════════════════════════════════════════════════════════════════
    //  Tìm kiếm tin nhắn + tên
    // ══════════════════════════════════════════════════════════════════════════
    private void setupSearch() {
        // Bật/tắt thanh tìm kiếm
        btnSearchToggle.setOnClickListener(v -> {
            if (layoutSearchBar.getVisibility() == View.VISIBLE) {
                closeSearch();
            } else {
                layoutSearchBar.setVisibility(View.VISIBLE);
                etSearchMessages.requestFocus();
            }
        });

        btnSearchClose.setOnClickListener(v -> closeSearch());

        etSearchMessages.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                performSearch(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSearchPrev.setOnClickListener(v -> navigateSearch(-1));
        btnSearchNext.setOnClickListener(v -> navigateSearch(+1));
    }

    /**
     * Tìm kiếm đa năng:
     *  1. Nếu query khớp tên đối phương → cuộn lên đầu để thấy tên trên header.
     *  2. Tìm trong nội dung tin nhắn  → highlight + điều hướng từng kết quả.
     */
    private void performSearch(String query) {
        searchPositions.clear();
        currentSearchIdx = -1;

        if (query.isEmpty()) {
            layoutSearchNav.setVisibility(View.GONE);
            chatAdapter.setSearchQuery("");
            chatAdapter.notifyDataSetChanged();
            return;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);

        // Kiểm tra tên đối phương
        boolean nameMatch = partnerName != null
                && partnerName.toLowerCase(Locale.ROOT).contains(lowerQuery);

        // Tìm trong nội dung tin nhắn
        for (int i = 0; i < allMessages.size(); i++) {
            ChatMessage msg = allMessages.get(i);
            if (msg.isRecalled()) continue;
            String content = msg.getContent();
            if (content != null && content.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                searchPositions.add(i);
            }
        }

        // Highlight trong adapter
        chatAdapter.setSearchQuery(query);
        chatAdapter.notifyDataSetChanged();

        int total = searchPositions.size();

        if (nameMatch && total == 0) {
            // Chỉ khớp tên — thông báo và tô sáng tên
            layoutSearchNav.setVisibility(View.VISIBLE);
            tvSearchResultInfo.setText("Tên: " + partnerName);
            tvPartnerName.setBackgroundColor(0x55FFFF00);
        } else if (total > 0) {
            // Có tin nhắn khớp
            tvPartnerName.setBackgroundColor(0x00000000);
            layoutSearchNav.setVisibility(View.VISIBLE);
            // Bắt đầu từ kết quả cuối cùng (mới nhất)
            currentSearchIdx = total - 1;
            updateSearchNavUI(nameMatch ? total + " tin  |  tên: " + partnerName
                    : String.valueOf(total) + " kết quả");
            scrollToSearchResult(currentSearchIdx);
        } else if (nameMatch) {
            layoutSearchNav.setVisibility(View.VISIBLE);
            tvSearchResultInfo.setText("Tên: " + partnerName);
            tvPartnerName.setBackgroundColor(0x55FFFF00);
        } else {
            layoutSearchNav.setVisibility(View.VISIBLE);
            tvSearchResultInfo.setText("Không tìm thấy");
        }
    }

    private void navigateSearch(int direction) {
        if (searchPositions.isEmpty()) return;
        currentSearchIdx += direction;
        if (currentSearchIdx < 0) currentSearchIdx = searchPositions.size() - 1;
        if (currentSearchIdx >= searchPositions.size()) currentSearchIdx = 0;
        updateSearchNavUI((currentSearchIdx + 1) + " / " + searchPositions.size());
        scrollToSearchResult(currentSearchIdx);
    }

    private void updateSearchNavUI(String text) {
        tvSearchResultInfo.setText(text);
    }

    private void scrollToSearchResult(int idx) {
        if (idx < 0 || idx >= searchPositions.size()) return;
        rvMessages.scrollToPosition(searchPositions.get(idx));
    }

    private void closeSearch() {
        layoutSearchBar.setVisibility(View.GONE);
        layoutSearchNav.setVisibility(View.GONE);
        etSearchMessages.setText("");
        searchPositions.clear();
        currentSearchIdx = -1;
        tvPartnerName.setBackgroundColor(0x00000000);
        chatAdapter.setSearchQuery("");
        chatAdapter.notifyDataSetChanged();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Chặn 2 chiều (real-time)
    // ══════════════════════════════════════════════════════════════════════════
    private void listenForBlockStatus() {
        // Chiều 1: tôi chặn đối phương
        blockListenerMine = db.collection("blocks")
                .document(currentUserId + "_" + partnerId)
                .addSnapshotListener((doc, e) -> {
                    iBlockedPartner = doc != null && doc.exists();
                    updateBlockUI();
                });

        // Chiều 2: đối phương chặn tôi
        blockListenerPartner = db.collection("blocks")
                .document(partnerId + "_" + currentUserId)
                .addSnapshotListener((doc, e) -> {
                    partnerBlockedMe = doc != null && doc.exists();
                    updateBlockUI();
                });
    }

    private void updateBlockUI() {
        boolean blocked = isAnyoneBlocked();

        if (tvBlockedBanner != null) {
            if (partnerBlockedMe && !iBlockedPartner) {
                tvBlockedBanner.setText("🚫 Bạn đã bị người này chặn. Không thể gửi tin nhắn.");
                tvBlockedBanner.setVisibility(View.VISIBLE);
            } else if (iBlockedPartner) {
                tvBlockedBanner.setText("🚫 Bạn đang chặn người này. Hãy bỏ chặn để nhắn tin.");
                tvBlockedBanner.setVisibility(View.VISIBLE);
            } else {
                tvBlockedBanner.setVisibility(View.GONE);
            }
        }

        if (etMessage != null) {
            etMessage.setEnabled(!blocked);
            etMessage.setHint(blocked ? "Không thể gửi tin nhắn" : "Nhập tin nhắn...");
        }
        updateSendButtonState();
        if (btnAddMedia != null) btnAddMedia.setEnabled(!blocked);
    }

    private boolean isAnyoneBlocked() {
        return iBlockedPartner || partnerBlockedMe;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Thu hồi tin nhắn
    // ══════════════════════════════════════════════════════════════════════════
    private void recallMessage(String messageId, ChatMessage msg) {
        if (!msg.getSenderId().equals(currentUserId)) return; // chỉ thu hồi tin của mình

        new AlertDialog.Builder(this)
                .setTitle("Thu hồi tin nhắn")
                .setMessage("Tin nhắn sẽ bị thu hồi với cả hai phía và không thể hoàn tác.")
                .setPositiveButton("Thu hồi", (d, w) ->
                        db.collection("chat_rooms").document(roomId)
                                .collection("messages").document(messageId)
                                .update(
                                        "recalled",     true,
                                        "content",      "",
                                        "imageUrl",     "",
                                        "videoUrl",     "",
                                        "thumbnailUrl", ""
                                )
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this, "Đã thu hồi tin nhắn",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Lỗi: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show())
                )
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Setup Chat (adapter + listeners)
    // ══════════════════════════════════════════════════════════════════════════
    private void setupChat() {
        chatAdapter = new ChatAdapter();

        // Mở media toàn màn hình
        chatAdapter.setOnMediaClickListener(this::openFullscreenMedia);

        // Long-press → thu hồi tin nhắn (chỉ tin của mình)
        chatAdapter.setOnMessageLongClickListener((messageId, message) -> {
            if (message.getSenderId().equals(currentUserId)) {
                recallMessage(messageId, message);
            }
        });

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(chatAdapter);
        rvMessages.setHasFixedSize(true);
    }

    private void openFullscreenMedia(ChatMessage clickedMsg) {
        ArrayList<String>  urls     = new ArrayList<>();
        ArrayList<Boolean> isVideos = new ArrayList<>();
        int startPos = 0;

        for (ChatMessage msg : allMessages) {
            if (msg.isRecalled()) continue;
            if (msg.isVideo() && msg.getVideoUrl() != null && !msg.getVideoUrl().isEmpty()) {
                if (msg.getVideoUrl().equals(clickedMsg.getVideoUrl())) startPos = urls.size();
                urls.add(msg.getVideoUrl());
                isVideos.add(true);
            } else if (!msg.isVideo() && msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
                if (msg.getImageUrl().equals(clickedMsg.getImageUrl())) startPos = urls.size();
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
                    allMessages.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) {
                            msg.setMessageId(doc.getId());
                            allMessages.add(msg);
                        }
                    }
                    chatAdapter.submitList(new ArrayList<>(allMessages), this::scrollToBottom);
                    updateReadStatus();

                    // Refresh highlight nếu đang tìm kiếm
                    if (layoutSearchBar != null
                            && layoutSearchBar.getVisibility() == View.VISIBLE
                            && etSearchMessages != null) {
                        String q = etSearchMessages.getText().toString().trim();
                        if (!q.isEmpty()) performSearch(q);
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Media Picker
    // ══════════════════════════════════════════════════════════════════════════
    private void applyTabStyle() {
        if (isVideoMode) {
            ivTabImage.setAlpha(0.45f); indicatorImage.setVisibility(View.INVISIBLE);
            ivTabVideo.setAlpha(1.0f);  indicatorVideo.setVisibility(View.VISIBLE);
        } else {
            ivTabImage.setAlpha(1.0f);  indicatorImage.setVisibility(View.VISIBLE);
            ivTabVideo.setAlpha(0.45f); indicatorVideo.setVisibility(View.INVISIBLE);
        }
    }

    private void reloadPickerContent() {
        if (mediaPickerAdapter == null) return;
        new Thread(() -> {
            mediaPickerAdapter.loadFromDevice(this, isVideoMode);
            runOnUiThread(() -> {
                if (!isFinishing() && !isDestroyed()) mediaPickerAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void setupMediaPicker() {
        if (rvMediaPicker == null) return;
        mediaPickerAdapter = new MediaPickerAdapter();
        rvMediaPicker.setLayoutManager(new GridLayoutManager(this, 3));
        rvMediaPicker.setAdapter(mediaPickerAdapter);

        mediaPickerAdapter.setOnMediaSelectedListener(selected -> {
            List<MediaPickerAdapter.MediaItem> toKeep = new ArrayList<>();
            for (MediaPickerAdapter.MediaItem item : pendingMedia) {
                if (item.isVideo != isVideoMode) toKeep.add(item);
            }
            pendingMedia.clear();
            pendingMedia.addAll(toKeep);
            pendingMedia.addAll(selected);
            updatePickerSendButton();
            updateSendButtonState();
        });

        if (btnPickerSend != null) {
            btnPickerSend.setOnClickListener(v -> {
                if (!pendingMedia.isEmpty()) handleSendMessage();
                closeMediaPicker();
            });
        }
    }

    private void updatePickerSendButton() {
        if (btnPickerSend == null) return;
        int total = pendingMedia.size();
        btnPickerSend.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        if (total > 0) btnPickerSend.setText("Gửi " + total);
    }

    private void checkPermissionAndOpen() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            boolean hi = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            boolean hv = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)  == PackageManager.PERMISSION_GRANTED;
            if (!hi || !hv) { permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}); return; }
        } else {
            boolean hs = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!hs) { permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}); return; }
        }
        openMediaPicker(isVideoMode);
    }

    private void openMediaPicker(boolean videoMode) {
        if (layoutMediaPicker == null || mediaPickerAdapter == null) return;
        isPickerOpen = true;
        isVideoMode  = videoMode;
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
        if (layoutMediaPicker != null) layoutMediaPicker.setVisibility(View.GONE);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Gửi tin nhắn
    // ══════════════════════════════════════════════════════════════════════════
    private void handleSendMessage() {
        if (isAnyoneBlocked()) {
            Toast.makeText(this, "Không thể gửi tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }
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
        final int   total     = items.size();
        final int[] doneCount = {0};

        for (int i = 0; i < total; i++) {
            final int index = i;
            MediaPickerAdapter.MediaItem item = items.get(index);
            final String msgText = (index == 0) ? textContent : "";

            if (item.isVideo) {
                CloudinaryHelper.uploadVideo(getApplicationContext(), item.uri,
                        new CloudinaryHelper.OnUploadCallback() {
                            @Override public void onSuccess(String url) {
                                runOnUiThread(() -> { performSendMessage(msgText, null, url); finishOneUpload(doneCount, total); });
                            }
                            @Override public void onFailure(String error) {
                                runOnUiThread(() -> { Toast.makeText(ChatDetailActivity.this, "Lỗi video: " + error, Toast.LENGTH_SHORT).show(); finishOneUpload(doneCount, total); });
                            }
                        });
            } else {
                CloudinaryHelper.uploadImage(getApplicationContext(), item.uri,
                        new CloudinaryHelper.OnUploadCallback() {
                            @Override public void onSuccess(String url) {
                                runOnUiThread(() -> { performSendMessage(msgText, url, null); finishOneUpload(doneCount, total); });
                            }
                            @Override public void onFailure(String error) {
                                runOnUiThread(() -> { Toast.makeText(ChatDetailActivity.this, "Lỗi ảnh: " + error, Toast.LENGTH_SHORT).show(); finishOneUpload(doneCount, total); });
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
        msg.put("recalled",  false);

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
                    String last = videoUrl != null ? "[Video]"
                            : imageUrl != null ? "[Hình ảnh]"
                            : content;
                    db.collection("chat_rooms").document(roomId)
                            .update("lastMessage", last,
                                    "lastTimestamp", FieldValue.serverTimestamp());
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════
    private void updateSendButtonState() {
        if (btnSend == null) return;
        if (isAnyoneBlocked()) { btnSend.setEnabled(false); return; }
        btnSend.setEnabled(!pendingMedia.isEmpty()
                || (etMessage != null && !etMessage.getText().toString().trim().isEmpty()));
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

    // ══════════════════════════════════════════════════════════════════════════
    //  Menu (Chặn / Báo cáo)
    // ══════════════════════════════════════════════════════════════════════════
    private void showPopupMenu(View v) {
        String[] options = iBlockedPartner
                ? new String[]{"Bỏ chặn", "Báo cáo"}
                : new String[]{"Chặn người này", "Báo cáo"};

        new AlertDialog.Builder(this).setItems(options, (dialog, which) -> {
            if (which == 0) {
                if (iBlockedPartner) {
                    // Bỏ chặn
                    db.collection("blocks").document(currentUserId + "_" + partnerId)
                            .delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Đã bỏ chặn " + partnerName,
                                            Toast.LENGTH_SHORT).show());
                } else {
                    // Chặn
                    Map<String, Object> b = new HashMap<>();
                    b.put("blockerId", currentUserId);
                    b.put("blockedId", partnerId);
                    b.put("timestamp", FieldValue.serverTimestamp());
                    db.collection("blocks").document(currentUserId + "_" + partnerId)
                            .set(b)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Đã chặn " + partnerName,
                                            Toast.LENGTH_SHORT).show());
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

    // ══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void onBackPressed() {
        if (layoutSearchBar != null && layoutSearchBar.getVisibility() == View.VISIBLE) {
            closeSearch();
            return;
        }
        if (isPickerOpen) {
            closeMediaPicker();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener         != null) chatListener.remove();
        if (blockListenerMine    != null) blockListenerMine.remove();
        if (blockListenerPartner != null) blockListenerPartner.remove();
    }
}