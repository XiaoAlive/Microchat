package com.example.microchat;

import java.util.Date;

/**
 * 会话实体类，用于表示消息列表中的会话项
 */
public class Conversation {
    private long id; // 对方用户ID
    private String name; // 对方用户名
    private String avatarUrl; // 对方头像URL
    private String lastMessage; // 最后一条消息内容
    private long lastMessageTime; // 最后一条消息时间
    private int unreadCount; // 未读消息数量

    public Conversation() {
    }

    public Conversation(long id, String name, String avatarUrl, String lastMessage, long lastMessageTime, int unreadCount) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * 获取格式化后的时间字符串，用于显示在列表项中
     * @return 格式化后的时间字符串
     */
    public String getFormattedTime() {
        Date date = new Date(lastMessageTime);
        Date now = new Date();
        long diff = now.getTime() - date.getTime();
        long days = diff / (1000 * 60 * 60 * 24);
        
        if (days > 7) {
            // 超过一周，显示具体日期
            return (date.getMonth() + 1) + "-" + date.getDate();
        } else if (days > 0) {
            // 一周内，显示星期
            String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            return weekdays[date.getDay()];
        } else {
            // 今天，显示时间
            int hours = date.getHours();
            int minutes = date.getMinutes();
            return String.format("%02d:%02d", hours, minutes);
        }
    }

    /**
     * 获取未读消息数的显示字符串
     * @return 未读消息数的显示字符串，如"1"、"5"或"99+"
     */
    public String getUnreadCountDisplay() {
        if (unreadCount <= 0) {
            return "";
        } else if (unreadCount > 99) {
            return "99+";
        } else {
            return String.valueOf(unreadCount);
        }
    }
}