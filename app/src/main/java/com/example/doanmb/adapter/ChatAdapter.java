package com.example.doanmb.adapter;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.doanmb.R;
import com.example.doanmb.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int TYPE_SENT     = 1;
    private static final int TYPE_RECEIVED = 2;

    private final String currentUserId;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Query tìm kiếm hiện tại (dùng để highlight)
    private String searchQuery = "";

    // ── Listeners ─────────────────────────────────────────────────────────────

    public interface OnMediaClickListener {
        void onMediaClick(ChatMessage message);
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(String messageId, ChatMessage message);
    }

    private OnMediaClickListener mediaClickListener;
    private OnMessageLongClickListener messageLongClickListener;

    public void setOnMediaClickListener(OnMediaClickListener l)           { this.mediaClickListener = l; }
    public void setOnMessageLongClickListener(OnMessageLongClickListener l) { this.messageLongClickListener = l; }

    /** Đặt từ khoá tìm kiếm để highlight trong bubble tin nhắn */
    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query : "";
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    public ChatAdapter() {
        super(new MessageDiffCallback());
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    public void submitList(List<ChatMessage> list, Runnable commitCallback) {
        super.submitList(list, commitCallback);
    }

    // ── ViewType ──────────────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    // ── onCreateViewHolder ────────────────────────────────────────────────────

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentVH(inf.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new ReceivedVH(inf.inflate(R.layout.item_message_received, parent, false));
        }
    }

    // ── onBindViewHolder ──────────────────────────────────────────────────────

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = getItem(position);

        String time = msg.getTimestamp() != null
                ? dateFormat.format(msg.getTimestamp().toDate())
                : dateFormat.format(new Date());

        if (holder instanceof SentVH) {
            bindSent((SentVH) holder, msg, time);
        } else {
            bindReceived((ReceivedVH) holder, msg, time);
        }
    }

    private void bindSent(SentVH h, ChatMessage msg, String time) {
        h.tvTime.setText(time);

        if (msg.isRecalled()) {
            // ── Thu hồi ──
            h.tvMessage.setText("Tin nhắn đã bị thu hồi");
            h.tvMessage.setTypeface(null, Typeface.ITALIC);
            h.tvMessage.setTextColor(0xFFBDBDBD);
            h.tvMessage.setVisibility(View.VISIBLE);
            h.tvStatus.setText("");
            h.cardImage.setVisibility(View.GONE);
            h.itemView.setOnLongClickListener(null);
        } else {
            // ── Bình thường ──
            h.tvMessage.setTypeface(null, Typeface.NORMAL);
            h.tvMessage.setTextColor(0xFFFFFFFF);

            String content = msg.getContent();
            if (content != null && !content.isEmpty()) {
                h.tvMessage.setVisibility(View.VISIBLE);
                h.tvMessage.setText(highlightText(content, 0xFFFFFF00)); // vàng
            } else {
                h.tvMessage.setVisibility(View.GONE);
            }

            // Trạng thái gửi
            if (msg.getTimestamp() == null) {
                h.tvStatus.setText("• Đang gửi...");
            } else {
                if      (msg.getStatus() == 0) h.tvStatus.setText("• Đã gửi");
                else if (msg.getStatus() == 1) h.tvStatus.setText("• Đã nhận");
                else if (msg.getStatus() == 2) h.tvStatus.setText("• Đã xem");
            }

            // Media
            if (msg.isVideo()) {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon,
                        msg.getThumbnailUrl() != null ? msg.getThumbnailUrl()
                                : cloudinaryThumbnail(msg.getVideoUrl()), true);
            } else {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon, msg.getImageUrl(), false);
            }
            bindMediaClick(h.cardImage, msg);

            // Long-press thu hồi (chỉ tin mình gửi)
            if (messageLongClickListener != null && msg.getMessageId() != null) {
                h.itemView.setOnLongClickListener(v -> {
                    messageLongClickListener.onMessageLongClick(msg.getMessageId(), msg);
                    return true;
                });
            } else {
                h.itemView.setOnLongClickListener(null);
            }
        }
    }

    private void bindReceived(ReceivedVH h, ChatMessage msg, String time) {
        h.tvTime.setText(time);

        if (msg.isRecalled()) {
            // ── Thu hồi ──
            h.tvMessage.setText("Tin nhắn đã bị thu hồi");
            h.tvMessage.setTypeface(null, Typeface.ITALIC);
            h.tvMessage.setTextColor(0xFF9E9E9E);
            h.tvMessage.setVisibility(View.VISIBLE);
            h.cardImage.setVisibility(View.GONE);
            h.itemView.setOnLongClickListener(null);
        } else {
            // ── Bình thường ──
            h.tvMessage.setTypeface(null, Typeface.NORMAL);
            h.tvMessage.setTextColor(0xFF1A1A2E); // màu mặc định nhận

            String content = msg.getContent();
            if (content != null && !content.isEmpty()) {
                h.tvMessage.setVisibility(View.VISIBLE);
                h.tvMessage.setText(highlightText(content, 0xFFFFEB3B)); // vàng đậm hơn cho nền sáng
            } else {
                h.tvMessage.setVisibility(View.GONE);
            }

            // Media
            if (msg.isVideo()) {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon,
                        msg.getThumbnailUrl() != null ? msg.getThumbnailUrl()
                                : cloudinaryThumbnail(msg.getVideoUrl()), true);
            } else {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon, msg.getImageUrl(), false);
            }
            bindMediaClick(h.cardImage, msg);
            h.itemView.setOnLongClickListener(null);
        }
    }

    // ── Highlight từ khoá tìm kiếm trong text ────────────────────────────────

    /**
     * Bọc các từ khớp với searchQuery bằng BackgroundColorSpan màu highlightColor.
     * Nếu searchQuery rỗng trả về text gốc.
     */
    private CharSequence highlightText(String text, int highlightColor) {
        if (searchQuery == null || searchQuery.isEmpty()) return text;

        String lowerText  = text.toLowerCase(Locale.ROOT);
        String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);

        SpannableString spannable = new SpannableString(text);
        int start = 0;
        while (true) {
            int idx = lowerText.indexOf(lowerQuery, start);
            if (idx == -1) break;
            spannable.setSpan(
                    new BackgroundColorSpan(highlightColor),
                    idx, idx + lowerQuery.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = idx + lowerQuery.length();
        }
        return spannable;
    }

    // ── Media helpers ─────────────────────────────────────────────────────────

    private void bindMediaClick(CardView card, ChatMessage msg) {
        if (msg.isRecalled()) {
            card.setOnClickListener(null);
            card.setClickable(false);
            return;
        }
        boolean hasMedia =
                (msg.isVideo()  && msg.getVideoUrl()  != null && !msg.getVideoUrl().isEmpty())
                        || (!msg.isVideo() && msg.getImageUrl() != null && !msg.getImageUrl().isEmpty());
        if (!hasMedia) {
            card.setOnClickListener(null);
            card.setClickable(false);
            return;
        }
        card.setOnClickListener(v -> {
            if (mediaClickListener != null) mediaClickListener.onMediaClick(msg);
        });
    }

    private void loadMedia(CardView card, ImageView iv, ImageView playIcon,
                           String url, boolean isVideo) {
        if (url != null && !url.isEmpty()) {
            card.setVisibility(View.VISIBLE);
            String optimized = url.contains("cloudinary.com")
                    ? url.replace("/upload/", "/upload/w_400,c_scale,q_auto,f_auto/") : url;
            Glide.with(iv.getContext())
                    .load(optimized)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(400)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .thumbnail(0.1f)
                    .into(iv);
            if (playIcon != null) playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        } else {
            card.setVisibility(View.GONE);
            if (playIcon != null) playIcon.setVisibility(View.GONE);
        }
    }

    private String cloudinaryThumbnail(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("cloudinary.com")) return null;
        return videoUrl
                .replace("/upload/", "/upload/so_0,w_400,c_fill,q_auto/")
                .replaceAll("\\.(mp4|mov|avi|mkv|webm)$", ".jpg");
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    static class SentVH extends RecyclerView.ViewHolder {
        TextView  tvMessage, tvTime, tvStatus;
        ImageView ivImage, ivPlayIcon;
        CardView  cardImage;

        SentVH(@NonNull View v) {
            super(v);
            tvMessage  = v.findViewById(R.id.tv_message);
            tvTime     = v.findViewById(R.id.tv_time);
            tvStatus   = v.findViewById(R.id.tv_status);
            ivImage    = v.findViewById(R.id.iv_message_image);
            ivPlayIcon = v.findViewById(R.id.iv_play_icon);
            cardImage  = v.findViewById(R.id.card_image);
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        TextView  tvMessage, tvTime;
        ImageView ivImage, ivPlayIcon;
        CardView  cardImage;

        ReceivedVH(@NonNull View v) {
            super(v);
            tvMessage  = v.findViewById(R.id.tv_message);
            tvTime     = v.findViewById(R.id.tv_time);
            ivImage    = v.findViewById(R.id.iv_message_image);
            ivPlayIcon = v.findViewById(R.id.iv_play_icon);
            cardImage  = v.findViewById(R.id.card_image);
        }
    }

    // ── DiffCallback ──────────────────────────────────────────────────────────

    static class MessageDiffCallback extends DiffUtil.ItemCallback<ChatMessage> {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            if (a.getMessageId() != null && b.getMessageId() != null)
                return a.getMessageId().equals(b.getMessageId());
            return a.getContent() != null && a.getContent().equals(b.getContent())
                    && a.getSenderId().equals(b.getSenderId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
            return a.getStatus() == b.getStatus()
                    && a.isRecalled() == b.isRecalled()
                    && (a.getTimestamp() != null && a.getTimestamp().equals(b.getTimestamp()));
        }
    }
}