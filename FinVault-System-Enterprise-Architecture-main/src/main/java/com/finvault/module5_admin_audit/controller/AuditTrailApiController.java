package com.finvault.module5_admin_audit.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module5_admin_audit.dto.AuditLogFilterRequest;
import com.finvault.module5_admin_audit.dto.AuditLogResponseDto;
import com.finvault.module5_admin_audit.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditTrailApiController: REST API Giám sát bảo mật và truy vết kiểm toán thời gian thực
 *
 * Tính năng chính (Dành cho ROLE_SUPER_ADMIN và ROLE_SYSTEM_AUDITOR):
 * - Xem nhật ký kiểm toán (Audit Trail) phân trang và bộ lọc chuyên sâu
 * - Giám sát danh sách các tài khoản đang bị khóa do nhập sai mật khẩu quá 5 lần
 * - Cho phép Super Admin mở khóa tài khoản thủ công trực tiếp trên màn hình chính
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
public class AuditTrailApiController {

    private final AuditLogService auditLogService;

    public AuditTrailApiController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Lấy danh sách vết kiểm toán phân trang
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDto>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogResponseDto> logs = auditLogService.getAuditLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Nhật ký kiểm toán hệ thống", logs));
    }

    /**
     * Lọc vết kiểm toán theo điều kiện nâng cao (Actor, Module, Action, Ngày bắt đầu, Ngày kết thúc)
     */
    @PostMapping("/logs/filter")
    public ResponseEntity<ApiResponse<Page<AuditLogResponseDto>>> filterAuditLogs(
            @RequestBody @Valid AuditLogFilterRequest filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogResponseDto> logs = auditLogService.filterAuditLogs(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Kết quả lọc vết kiểm toán", logs));
    }

    /**
     * Lấy 50 vết kiểm toán gần nhất phục vụ bảng giám sát trực tiếp trên màn hình chính
     */
    @GetMapping("/logs/recent")
    public ResponseEntity<ApiResponse<List<AuditLogResponseDto>>> getRecentAuditLogs() {
        List<AuditLogResponseDto> logs = auditLogService.getRecentAuditLogs();
        return ResponseEntity.ok(ApiResponse.success("50 vết kiểm toán gần nhất", logs));
    }

    /**
     * Giám sát các tài khoản người dùng đang bị khóa do cơ chế Brute-force Protection
     */
    @GetMapping("/locked-accounts")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getCurrentlyLockedAccounts() {
        List<UserResponseDto> lockedUsers = auditLogService.getCurrentlyLockedAccounts();
        return ResponseEntity.ok(ApiResponse.success("Danh sách tài khoản đang bị tạm khóa", lockedUsers));
    }

    /**
     * Mở khóa tài khoản thủ công (Dành riêng cho Super Admin)
     */
    @PostMapping("/locked-accounts/{username}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUserAccount(@PathVariable String username) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        auditLogService.unlockUserAccount(username, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa tài khoản thành công cho: " + username, null));
    }
}
