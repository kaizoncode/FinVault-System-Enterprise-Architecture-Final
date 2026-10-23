package com.finvault.module5_admin_audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfigRequest: DTO cập nhật tham số cấu hình hệ thống từ thanh công cụ Live Edit Mode
 * ==============================================================================
 */
public class SystemConfigRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Khóa cấu hình không được để trống")
    private String configKey;

    @NotBlank(message = "Giá trị cấu hình không được để trống")
    @Size(max = 1000, message = "Giá trị cấu hình tối đa 1000 ký tự")
    private String configValue;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;

    public SystemConfigRequest() {
    }

    public SystemConfigRequest(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
