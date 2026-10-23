package com.finvault.module5_admin_audit.dto;

import com.finvault.module5_admin_audit.entity.SystemNotification;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotificationResponseDto: DTO phản hồi thông tin thông báo/banner hệ thống
 * ==============================================================================
 */
public class SystemNotificationResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String notificationType;
    private Boolean isActive;
    private String broadcastToRoles;
    private Long targetUserId;
    private String targetUsername;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SystemNotificationResponseDto() {
    }

    public static SystemNotificationResponseDto fromEntity(SystemNotification entity) {
        if (entity == null) return null;
        SystemNotificationResponseDto dto = new SystemNotificationResponseDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setNotificationType(entity.getNotificationType());
        dto.setIsActive(entity.getIsActive());
        dto.setBroadcastToRoles(entity.getBroadcastToRoles());
        dto.setTargetUserId(entity.getTargetUserId());
        dto.setTargetUsername(entity.getTargetUsername());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
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
}
