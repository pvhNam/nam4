package com.example.doanmb.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class ChatMessage {

    public static final String TYPE_TEXT  = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_VIDEO = "video";

    private String messageId;
    private String senderId;
    private String content;
    private String imageUrl;
    private String videoUrl;
    private String thumbnailUrl;
    private String messageType;
    private Timestamp timestamp;
    private int status; // 0: Sent, 1: Delivered, 2: Read

    public ChatMessage() {}

    public ChatMessage(String senderId, String content, Timestamp timestamp, int status) {
        this.senderId    = senderId;
        this.content     = content;
        this.timestamp   = timestamp;
        this.status      = status;
        this.messageType = TYPE_TEXT;
    }

    @Exclude
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    @Exclude
    public boolean isVideo() { return TYPE_VIDEO.equals(messageType); }

    @Exclude
    public boolean isImage() { return TYPE_IMAGE.equals(messageType) || (imageUrl != null && !imageUrl.isEmpty()); }
}