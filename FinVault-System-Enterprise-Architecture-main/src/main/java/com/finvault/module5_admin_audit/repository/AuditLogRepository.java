package com.finvault.module5_admin_audit.repository;

import com.finvault.module5_admin_audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogRepository: Tầng truy xuất dữ liệu vết nhật ký kiểm toán hệ thống
 * ==============================================================================
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    List<AuditLog> findAllByActorUsernameOrderByTimestampDesc(String actorUsername);

    List<AuditLog> findAllByTargetModuleOrderByTimestampDesc(String targetModule);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:actorUsername IS NULL OR LOWER(a.actorUsername) LIKE LOWER(CONCAT('%', :actorUsername, '%'))) AND " +
           "(:targetModule IS NULL OR a.targetModule = :targetModule) AND " +
           "(:action IS NULL OR LOWER(a.action) LIKE LOWER(CONCAT('%', :action, '%'))) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> filterAuditLogs(
            @Param("actorUsername") String actorUsername,
            @Param("targetModule") String targetModule,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    long countByTimestampAfter(LocalDateTime timestamp);
}
