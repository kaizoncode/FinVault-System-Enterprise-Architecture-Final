package com.finvault.module5_admin_audit.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLog Entity: Lưu trữ vết nhật ký kiểm toán hệ thống (Audit Trail)
 *
 * Tính năng chính:
 * - Ghi nhận danh tính người thao tác (actor_username, actor_role)
 * - Mô tả chi tiết hành động (action), phân hệ mục tiêu (target_module)
 * - Lưu trữ trạng thái trước và sau khi thay đổi (old_value, new_value)
 * - Ghi nhận dấu vết mạng (ip_address, user_agent, timestamp)
 * - Đánh giá trạng thái thành công/thất bại (status, failure_reason)
 * ==============================================================================
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "actor_username", length = 50, nullable = false)
    private String actorUsername;

    @Column(name = "actor_role", length = 255)
    private String actorRole;

    @Column(name = "action", length = 100, nullable = false)
    private String action;

    @Column(name = "target_module", length = 50, nullable = false)
    private String targetModule;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    @Column(name = "entity_id")
    private Long entityId;

    @Lob
    @Column(name = "old_value")
    private String oldValue;

    @Lob
    @Column(name = "new_value")
    private String newValue;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    @Column(name = "status", length = 20)
    private String status = "SUCCESS";

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public AuditLog() {
    }

    public AuditLog(String actorUsername, String actorRole, String action, String targetModule,
                    String entityName, Long entityId, String oldValue, String newValue,
                    String ipAddress, String userAgent, String status, String failureReason) {
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.action = action;
        this.targetModule = targetModule;
        this.entityName = entityName;
        this.entityId = entityId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = LocalDateTime.now();
        this.status = (status != null) ? status : "SUCCESS";
        this.failureReason = failureReason;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return id != null && Objects.equals(id, auditLog.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", actorUsername='" + actorUsername + '\'' +
                ", action='" + action + '\'' +
                ", targetModule='" + targetModule + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}
