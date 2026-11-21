package com.hr.model;

import java.time.LocalDateTime;

public class Notification {
    private int notificationId;
    private String senderId;
    private String receiverId;    
    private String title;
    private String content;
    private LocalDateTime createdAt;

    private boolean read;

    public Notification() { }

    public boolean isBroadcast() {
        return receiverId == null;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int v) {
        this.notificationId = v;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String v) {
        this.senderId = v;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String v) {
        this.receiverId = v;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String v) {
        this.title = v;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String v) {
        this.content = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        this.createdAt = v;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean v) {
        this.read = v;
    }
}