package com.finvault.module5_admin_audit.service;

import com.finvault.module5_admin_audit.dto.SystemNotificationRequest;
import com.finvault.module5_admin_audit.dto.SystemNotificationResponseDto;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotificationService: Interface quản lý thông báo/banner chạy trên giao diện chính
 * ==============================================================================
 */
public interface SystemNotificationService {

    List<SystemNotificationResponseDto> getActiveNotifications();

    List<SystemNotificationResponseDto> getAllNotifications();

    SystemNotificationResponseDto createNotification(String adminUsername, SystemNotificationRequest request);

    SystemNotificationResponseDto updateNotification(Long id, String adminUsername, SystemNotificationRequest request);

    void toggleNotificationStatus(Long id, String adminUsername, boolean active);

    void deleteNotification(Long id, String adminUsername);
}
