package com.finvault.module5_admin_audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotificationRequest: DTO tạo hoặc cập nhật thông báo/banner toàn hệ thống
 * ==============================================================================
 */
public class SystemNotificationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tiêu đề thông báo không được để trống")
    @Size(max = 200, message = "Tiêu đề thông báo tối đa 200 ký tự")
    private String title;

    @NotBlank(message = "Nội dung thông báo không được để trống")
    @Size(max = 2000, message = "Nội dung thông báo tối đa 2000 ký tự")
    private String content;

    private String notificationType = "INFO";

    private Boolean isActive = true;

    private String broadcastToRoles = "ALL";

    public SystemNotificationRequest() {
    }

    public SystemNotificationRequest(String title, String content, String notificationType, Boolean isActive, String broadcastToRoles) {
        this.title = title;
        this.content = content;
        this.notificationType = (notificationType != null) ? notificationType : "INFO";
        this.isActive = (isActive != null) ? isActive : true;
        this.broadcastToRoles = (broadcastToRoles != null) ? broadcastToRoles : "ALL";
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
}
