package com.finvault.module5_admin_audit.repository;

import com.finvault.module5_admin_audit.entity.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemNotificationRepository: Tầng truy xuất thông báo/banner toàn hệ thống
 * ==============================================================================
 */
@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {

    List<SystemNotification> findAllByIsActiveTrueOrderByCreatedAtDesc();

    List<SystemNotification> findAllByOrderByCreatedAtDesc();
}
