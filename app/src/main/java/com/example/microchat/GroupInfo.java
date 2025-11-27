package com.example.microchat;

/**
 * 群聊信息数据模型
 */
public class GroupInfo {
    private String name;        // 群聊名称
    private String description; // 群聊描述
    private int memberCount;    // 成员数量
    private int onlineCount;    // 在线人数
    private String type;        // 群聊类型
    private int avatarResId;    // 头像资源ID

    public GroupInfo() {
    }

    public GroupInfo(String name, String description, int memberCount, int onlineCount, String type, int avatarResId) {
        this.name = name;
        this.description = description;
        this.memberCount = memberCount;
        this.onlineCount = onlineCount;
        this.type = type;
        this.avatarResId = avatarResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public int getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount) {
        this.onlineCount = onlineCount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAvatarResId() {
        return avatarResId;
    }

    public void setAvatarResId(int avatarResId) {
        this.avatarResId = avatarResId;
    }
}