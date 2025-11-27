package com.example.server.entity;

public class Contact {
    private Long id;
    private Long userId; // 所属用户ID
    private Long contactId; // 联系人用户ID
    private String remark; // 备注名

    // 构造方法、getter、setter省略

    public Contact() {}

    public Contact(Long id, Long userId, Long contactId, String remark) {
        this.id = id;
        this.userId = userId;
        this.contactId = contactId;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
