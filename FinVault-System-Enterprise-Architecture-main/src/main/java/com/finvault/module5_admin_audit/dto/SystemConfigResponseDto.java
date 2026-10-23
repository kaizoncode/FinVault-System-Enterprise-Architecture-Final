package com.finvault.module5_admin_audit.dto;

import com.finvault.module5_admin_audit.entity.SystemConfig;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfigResponseDto: DTO phản hồi chi tiết cấu hình hệ thống
 * ==============================================================================
 */
public class SystemConfigResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String configKey;
    private String configValue;
    private String configGroup;
    private String description;
    private Boolean isEditable;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public SystemConfigResponseDto() {
    }

    public static SystemConfigResponseDto fromEntity(SystemConfig entity) {
        if (entity == null) return null;
        SystemConfigResponseDto dto = new SystemConfigResponseDto();
        dto.setId(entity.getId());
        dto.setConfigKey(entity.getConfigKey());
        dto.setConfigValue(entity.getConfigValue());
        dto.setConfigGroup(entity.getConfigGroup());
        dto.setDescription(entity.getDescription());
        dto.setIsEditable(entity.getIsEditable());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
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
}
