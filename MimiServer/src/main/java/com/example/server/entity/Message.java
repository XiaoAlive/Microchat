package com.example.server.entity;

public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private String sendTime;
    private Boolean isRead; // 消息是否已读

    // 构造方法、getter、setter省略

    public Message() {}

    public Message(Long id, Long senderId, Long receiverId, String content, String sendTime) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.sendTime = sendTime;
        this.isRead = false; // 默认未读
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
