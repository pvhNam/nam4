package com.example.doanmb.model;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderId;
    private String content;
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

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
