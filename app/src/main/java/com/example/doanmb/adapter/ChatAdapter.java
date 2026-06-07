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
import java.util.Locale;

public class ChatAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private final String currentUserId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter() {
        super(new MessageDiffCallback());
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            return new SentViewHolder(inflater.inflate(R.layout.item_message_sent, parent, false));
        } else {
            return new ReceivedViewHolder(inflater.inflate(R.layout.item_message_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);

        // Fix lỗi treo Real-time: Nếu server chưa xác nhận (null), dùng giờ máy để hiện ngay
        String time = message.getTimestamp() != null
                ? dateFormat.format(message.getTimestamp().toDate())
                : dateFormat.format(new Date());

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            h.tvTime.setText(time);
            h.tvMessage.setText(message.getContent());
            h.tvMessage.setVisibility(message.getContent() != null && !message.getContent().isEmpty() ? View.VISIBLE : View.GONE);

            // Hiện trạng thái "Đang gửi" mượt mà
            if (message.getTimestamp() == null) {
                h.tvStatus.setText("• Đang gửi...");
            } else {
                if (message.getStatus() == 0) h.tvStatus.setText("• Đã gửi");
                else if (message.getStatus() == 1) h.tvStatus.setText("• Đã nhận");
                else if (message.getStatus() == 2) h.tvStatus.setText("• Đã xem");
            }
            displayImage(h.cardImage, h.ivImage, message.getImageUrl());
        } else {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            h.tvTime.setText(time);
            h.tvMessage.setText(message.getContent());
            h.tvMessage.setVisibility(message.getContent() != null && !message.getContent().isEmpty() ? View.VISIBLE : View.GONE);
            displayImage(h.cardImage, h.ivImage, message.getImageUrl());
        }
    }

    private void displayImage(CardView card, ImageView iv, String url) {
        if (url != null && !url.isEmpty()) {
            card.setVisibility(View.VISIBLE);
            // TỐI ƯU SIÊU TỐC: Cloudinary tự động trả về bản WebP rộng 400px cực nhẹ
            String optimizedUrl = url.contains("cloudinary.com")
                    ? url.replace("/upload/", "/upload/w_400,c_scale,q_auto,f_auto/") : url;

            Glide.with(iv.getContext())
                    .load(optimizedUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(400)
                    .placeholder(R.drawable.color_image_placeholder)
                    .thumbnail(0.1f)
                    .into(iv);
        } else {
            card.setVisibility(View.GONE);
        }
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvStatus;
        ImageView ivImage; CardView cardImage;
        SentViewHolder(View v) { super(v);
            tvMessage = v.findViewById(R.id.tv_message); tvTime = v.findViewById(R.id.tv_time);
            tvStatus = v.findViewById(R.id.tv_status); ivImage = v.findViewById(R.id.iv_message_image);
            cardImage = v.findViewById(R.id.card_image);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime; ImageView ivImage; CardView cardImage;
        ReceivedViewHolder(View v) { super(v);
            tvMessage = v.findViewById(R.id.tv_message); tvTime = v.findViewById(R.id.tv_time);
            ivImage = v.findViewById(R.id.iv_message_image); cardImage = v.findViewById(R.id.card_image);
        }
    }

    static class MessageDiffCallback extends DiffUtil.ItemCallback<ChatMessage> {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            // So sánh dựa trên ID của Firestore
            if (oldItem.getMessageId() != null && newItem.getMessageId() != null) {
                return oldItem.getMessageId().equals(newItem.getMessageId());
            }
            return oldItem.getContent().equals(newItem.getContent()) && oldItem.getSenderId().equals(newItem.getSenderId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.getStatus() == newItem.getStatus() &&
                    (oldItem.getTimestamp() != null && oldItem.getTimestamp().equals(newItem.getTimestamp()));
        }
    }
}