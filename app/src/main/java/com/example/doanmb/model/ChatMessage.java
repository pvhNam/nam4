package com.example.doanmb.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class ChatMessage {
    private String messageId;
    private String senderId;
    private String content;
    private String imageUrl;
    private Timestamp timestamp;
    private int status; // 0: Sent, 1: Delivered, 2: Read

    public ChatMessage() {
        // Required for Firestore
    }

    public ChatMessage(String senderId, String content, Timestamp timestamp, int status) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.status = status;
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

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
