package com.finvault.module5_admin_audit.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotification Entity: Lưu trữ thông báo/banner toàn hệ thống do Super Admin ban hành
 * ==============================================================================
 */
@Entity
@Table(name = "system_notifications")
public class SystemNotification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "notification_type", length = 30, nullable = false)
    private String notificationType = "INFO"; // INFO, WARNING, DANGER, SUCCESS

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "broadcast_to_roles", length = 255)
    private String broadcastToRoles = "ALL";

    @Column(name = "target_user_id")
    private Long targetUserId;

    @Column(name = "target_username", length = 50)
    private String targetUsername;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SystemNotification() {
    }

    public SystemNotification(String title, String content, String notificationType, Boolean isActive, String broadcastToRoles, String createdBy) {
        this.title = title;
        this.content = content;
        this.notificationType = (notificationType != null) ? notificationType : "INFO";
        this.isActive = (isActive != null) ? isActive : true;
        this.broadcastToRoles = broadcastToRoles;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public SystemNotification(String title, String content, String notificationType, Boolean isActive, String broadcastToRoles, Long targetUserId, String targetUsername, String createdBy) {
        this(title, content, notificationType, isActive, broadcastToRoles, createdBy);
        this.targetUserId = targetUserId;
        this.targetUsername = targetUsername;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getBroadcastToRoles() {
        return broadcastToRoles;
    }

    public void setBroadcastToRoles(String broadcastToRoles) {
        this.broadcastToRoles = broadcastToRoles;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemNotification that = (SystemNotification) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SystemNotification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", notificationType='" + notificationType + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
