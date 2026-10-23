package com.finvault.module5_admin_audit.service;

import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module5_admin_audit.dto.AuditLogFilterRequest;
import com.finvault.module5_admin_audit.dto.AuditLogResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogService: Interface quản trị vết kiểm toán, giám sát bảo mật và mở khóa tài khoản
 * ==============================================================================
 */
public interface AuditLogService {

    Page<AuditLogResponseDto> getAuditLogs(Pageable pageable);

    Page<AuditLogResponseDto> filterAuditLogs(AuditLogFilterRequest filter, Pageable pageable);

    List<AuditLogResponseDto> getRecentAuditLogs();

    List<UserResponseDto> getCurrentlyLockedAccounts();

    void unlockUserAccount(String targetUsername, String adminUsername);
}
