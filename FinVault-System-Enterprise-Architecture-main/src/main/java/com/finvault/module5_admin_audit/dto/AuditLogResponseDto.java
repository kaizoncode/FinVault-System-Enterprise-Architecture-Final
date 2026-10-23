package com.finvault.module5_admin_audit.dto;

import com.finvault.module5_admin_audit.entity.AuditLog;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogResponseDto: DTO phản hồi chi tiết bản ghi nhật ký kiểm toán hệ thống
 * ==============================================================================
 */
public class AuditLogResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String actorUsername;
    private String actorRole;
    private String action;
    private String targetModule;
    private String entityName;
    private Long entityId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
    private String status;
    private String failureReason;

    public AuditLogResponseDto() {
    }

    public static AuditLogResponseDto fromEntity(AuditLog entity) {
        if (entity == null) return null;
        AuditLogResponseDto dto = new AuditLogResponseDto();
        dto.setId(entity.getId());
        dto.setActorUsername(entity.getActorUsername());
        dto.setActorRole(entity.getActorRole());
        dto.setAction(entity.getAction());
        dto.setTargetModule(entity.getTargetModule());
        dto.setEntityName(entity.getEntityName());
        dto.setEntityId(entity.getEntityId());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setTimestamp(entity.getTimestamp());
        dto.setStatus(entity.getStatus());
        dto.setFailureReason(entity.getFailureReason());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
