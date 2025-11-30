package com.example.microchat;

public class Message {
    private Long id;//消息ID
    private Long senderId;//发送者ID
    private Long receiverId;//接收者ID
    private String contactName;//发出人的名字（用于兼容旧代码）
    private long time;//发出消息的时间
    private String content;//消息的内容

    // 默认构造函数，用于Gson解析
    public Message() {
    }

    // 兼容旧代码的构造函数
    public Message(String contactName, long time, String content) {
        this.contactName = contactName;
        this.time = time;
        this.content = content;
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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

// 现在使用Message类代替ChatMessage类
