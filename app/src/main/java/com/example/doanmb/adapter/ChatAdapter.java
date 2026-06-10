package com.example.doanmb.adapter;

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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter() {
        super(new MessageDiffCallback());
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    public void submitList(List<ChatMessage> list, Runnable commitCallback) {
        super.submitList(list, commitCallback);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

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

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = getItem(position);

        String time = msg.getTimestamp() != null
                ? dateFormat.format(msg.getTimestamp().toDate())
                : dateFormat.format(new Date());

        if (holder instanceof SentVH) {
            SentVH h = (SentVH) holder;
            h.tvTime.setText(time);

            String content = msg.getContent();
            h.tvMessage.setText(content);
            h.tvMessage.setVisibility(content != null && !content.isEmpty() ? View.VISIBLE : View.GONE);

            if (msg.getTimestamp() == null) {
                h.tvStatus.setText("• Đang gửi...");
            } else {
                if      (msg.getStatus() == 0) h.tvStatus.setText("• Đã gửi");
                else if (msg.getStatus() == 1) h.tvStatus.setText("• Đã nhận");
                else if (msg.getStatus() == 2) h.tvStatus.setText("• Đã xem");
            }

            // Hiển thị video hoặc ảnh
            if (msg.isVideo()) {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon,
                        msg.getThumbnailUrl() != null ? msg.getThumbnailUrl()
                                : CloudinaryThumbnail(msg.getVideoUrl()), true);
            } else {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon, msg.getImageUrl(), false);
            }

        } else {
            ReceivedVH h = (ReceivedVH) holder;
            h.tvTime.setText(time);

            String content = msg.getContent();
            h.tvMessage.setText(content);
            h.tvMessage.setVisibility(content != null && !content.isEmpty() ? View.VISIBLE : View.GONE);

            if (msg.isVideo()) {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon,
                        msg.getThumbnailUrl() != null ? msg.getThumbnailUrl()
                                : CloudinaryThumbnail(msg.getVideoUrl()), true);
            } else {
                loadMedia(h.cardImage, h.ivImage, h.ivPlayIcon, msg.getImageUrl(), false);
            }
        }
    }

    /** Load ảnh hoặc thumbnail video vào bubble tin nhắn */
    private void loadMedia(CardView card, ImageView iv, ImageView playIcon, String url, boolean isVideo) {
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

    /** Tạo thumbnail URL từ video URL Cloudinary */
    private String CloudinaryThumbnail(String videoUrl) {
        if (videoUrl == null || !videoUrl.contains("cloudinary.com")) return null;
        return videoUrl
                .replace("/upload/", "/upload/so_0,w_400,c_fill,q_auto/")
                .replaceAll("\\.(mp4|mov|avi|mkv|webm)$", ".jpg");
    }

    // ── ViewHolders ──────────────────────────────────────────────────────────

    static class SentVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvStatus;
        ImageView ivImage, ivPlayIcon;
        CardView cardImage;

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
        TextView tvMessage, tvTime;
        ImageView ivImage, ivPlayIcon;
        CardView cardImage;

        ReceivedVH(@NonNull View v) {
            super(v);
            tvMessage  = v.findViewById(R.id.tv_message);
            tvTime     = v.findViewById(R.id.tv_time);
            ivImage    = v.findViewById(R.id.iv_message_image);
            ivPlayIcon = v.findViewById(R.id.iv_play_icon);
            cardImage  = v.findViewById(R.id.card_image);
        }
    }

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
                    && (a.getTimestamp() != null && a.getTimestamp().equals(b.getTimestamp()));
        }
    }
}