package com.finvault.module5_admin_audit.service;

import com.finvault.common.BusinessException;
import com.finvault.module5_admin_audit.aop.Auditable;
import com.finvault.module5_admin_audit.dto.SystemNotificationRequest;
import com.finvault.module5_admin_audit.dto.SystemNotificationResponseDto;
import com.finvault.module5_admin_audit.entity.SystemNotification;
import com.finvault.module5_admin_audit.repository.SystemNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotificationServiceImpl: Triển khai phát thông báo toàn hệ thống (Live Admin CMS)
 * ==============================================================================
 */
@Service
@Transactional
public class SystemNotificationServiceImpl implements SystemNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationServiceImpl.class);

    private final SystemNotificationRepository notificationRepository;

    public SystemNotificationServiceImpl(SystemNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemNotificationResponseDto> getActiveNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return notificationRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .filter(n -> isVisibleToUser(n, auth))
                .map(SystemNotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    private boolean isVisibleToUser(SystemNotification n, Authentication auth) {
        if (n == null) {
            return false;
        }
        String broadcast = n.getBroadcastToRoles();
        boolean isBroadcastAll = (broadcast == null || broadcast.isBlank() || "ALL".equalsIgnoreCase(broadcast.trim()));
        boolean hasTargetUser = (n.getTargetUsername() != null && !n.getTargetUsername().isBlank());

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return isBroadcastAll && !hasTargetUser;
        }

        String currentUsername = auth.getName();
        Set<String> userRoles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (userRoles.contains("ROLE_SUPER_ADMIN")) {
            return true;
        }

        if (hasTargetUser && !n.getTargetUsername().trim().equalsIgnoreCase(currentUsername)) {
            return false;
        }

        if (isBroadcastAll) {
            return true;
        }

        String[] allowedRoles = broadcast.split(",");
        for (String role : allowedRoles) {
            String trimmed = role.trim();
            if (!trimmed.isEmpty()) {
                if (userRoles.contains(trimmed) || userRoles.contains("ROLE_" + trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemNotificationResponseDto> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(SystemNotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Auditable(action = "CREATE_SYSTEM_NOTIFICATION", module = "SYSTEM_NOTIFICATION", entityName = "system_notifications")
    public SystemNotificationResponseDto createNotification(String adminUsername, SystemNotificationRequest request) {
        log.info("Admin [{}] ban hành thông báo mới: [{}]", adminUsername, request.getTitle());

        SystemNotification notification = new SystemNotification(
                request.getTitle(),
                request.getContent(),
                request.getNotificationType(),
                request.getIsActive(),
                request.getBroadcastToRoles(),
                adminUsername
        );

        SystemNotification saved = notificationRepository.save(notification);
        log.info("Ban hành thành công thông báo ID: {}", saved.getId());
        return SystemNotificationResponseDto.fromEntity(saved);
    }

    @Override
    @Auditable(action = "UPDATE_SYSTEM_NOTIFICATION", module = "SYSTEM_NOTIFICATION", entityName = "system_notifications")
    public SystemNotificationResponseDto updateNotification(Long id, String adminUsername, SystemNotificationRequest request) {
        log.info("Admin [{}] cập nhật thông báo ID: {}", adminUsername, id);

        SystemNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo ID: " + id));

        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        if (request.getNotificationType() != null) {
            notification.setNotificationType(request.getNotificationType());
        }
        if (request.getIsActive() != null) {
            notification.setIsActive(request.getIsActive());
        }
        if (request.getBroadcastToRoles() != null) {
            notification.setBroadcastToRoles(request.getBroadcastToRoles());
        }
        notification.setUpdatedAt(LocalDateTime.now());

        SystemNotification updated = notificationRepository.save(notification);
        return SystemNotificationResponseDto.fromEntity(updated);
    }

    @Override
    @Auditable(action = "TOGGLE_NOTIFICATION_STATUS", module = "SYSTEM_NOTIFICATION", entityName = "system_notifications")
    public void toggleNotificationStatus(Long id, String adminUsername, boolean active) {
        log.info("Admin [{}] chuyển trạng thái thông báo ID: {} thành [{}]", adminUsername, id, active);
        SystemNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo ID: " + id));

        notification.setIsActive(active);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Auditable(action = "DELETE_SYSTEM_NOTIFICATION", module = "SYSTEM_NOTIFICATION", entityName = "system_notifications")
    public void deleteNotification(Long id, String adminUsername) {
        log.info("Admin [{}] xóa thông báo ID: {}", adminUsername, id);
        SystemNotification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo ID: " + id));

        notificationRepository.delete(notification);
    }
}
