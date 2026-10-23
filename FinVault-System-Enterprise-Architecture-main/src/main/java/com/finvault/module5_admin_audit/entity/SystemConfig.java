package com.finvault.module5_admin_audit.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfig Entity: Quản trị các tham số cấu hình động toàn hệ thống (Live Admin CMS)
 *
 * Tính năng chính:
 * - Khóa cấu hình (config_key) duy nhất
 * - Nhóm phân loại cấu hình (config_group: SECURITY_ALERT, FAMILY_RULE, SYSTEM_NOTIFICATION, GENERAL)
 * - Cho phép Super Admin chỉnh sửa trực tiếp (In-place editing) ngay trên màn hình chính
 * ==============================================================================
 */
@Entity
@Table(name = "system_configs")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "config_key", length = 100, nullable = false, unique = true)
    private String configKey;

    @Column(name = "config_value", length = 1000, nullable = false)
    private String configValue;

    @Column(name = "config_group", length = 50, nullable = false)
    private String configGroup;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_editable")
    private Boolean isEditable = true;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public SystemConfig() {
    }

    public SystemConfig(String configKey, String configValue, String configGroup, String description, Boolean isEditable, String updatedBy) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configGroup = configGroup;
        this.description = description;
        this.isEditable = (isEditable != null) ? isEditable : true;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEditable() {
        return isEditable;
    }

    public void setIsEditable(Boolean editable) {
        isEditable = editable;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
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
        SystemConfig that = (SystemConfig) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SystemConfig{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", configGroup='" + configGroup + '\'' +
                ", isEditable=" + isEditable +
                '}';
    }
}
