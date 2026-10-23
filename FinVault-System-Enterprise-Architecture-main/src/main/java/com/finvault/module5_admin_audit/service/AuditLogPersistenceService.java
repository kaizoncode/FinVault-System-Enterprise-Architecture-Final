package com.finvault.module5_admin_audit.service;

import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogPersistenceService: Lưu vết kiểm toán độc lập trong Transaction riêng biệt
 *
 * Tính năng chống mất mát Audit Trail (Propagation.REQUIRES_NEW):
 * - Khi luồng nghiệp vụ chính ném Exception và rollback, Transaction của AuditLog
 *   vẫn được commit độc lập vào cơ sở dữ liệu, đảm bảo mọi hành vi lỗi / tấn công
 *   đều được ghi vết toàn vẹn và không bao giờ bị rollback cuốn trôi.
 * ==============================================================================
 */
@Service
public class AuditLogPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPersistenceService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogPersistenceService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Lưu AuditLog trong một transaction mới hoàn toàn độc lập với transaction nghiệp vụ gọi nó
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(AuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        try {
            auditLogRepository.save(auditLog);
            log.debug("Đã lưu vết audit thành công trong transaction độc lập (Action: [{}], Status: [{}])",
                    auditLog.getAction(), auditLog.getStatus());
        } catch (Exception ex) {
            log.error("Lỗi khi ghi lưu vết audit log: {}", ex.getMessage(), ex);
        }
    }
}
