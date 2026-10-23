package com.finvault.module5_admin_audit.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module5_admin_audit.dto.DataRecoveryResponseDto;
import com.finvault.module5_admin_audit.service.SupportTicketRecoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * DataRecoveryApiController: REST API Khôi phục dữ liệu đã bị xóa mềm (Soft Delete Recovery)
 *
 * Tính năng chính (Dành riêng cho ROLE_SUPER_ADMIN):
 * - Quét và liệt kê toàn bộ các ví, giao dịch, ngân sách, mục tiêu bị xóa nhầm
 * - Khôi phục từng đối tượng dữ liệu về trạng thái khả dụng và tự động đồng bộ số dư ví
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/admin/recovery")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DataRecoveryApiController {

    private final SupportTicketRecoveryService recoveryService;

    public DataRecoveryApiController(SupportTicketRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    /**
     * Lấy toàn bộ danh sách các đối tượng dữ liệu đang bị xóa mềm
     */
    @GetMapping("/deleted-items")
    public ResponseEntity<ApiResponse<List<DataRecoveryResponseDto>>> getSoftDeletedItems() {
        List<DataRecoveryResponseDto> items = recoveryService.getSoftDeletedItems();
        return ResponseEntity.ok(ApiResponse.success("Danh sách đối tượng dữ liệu xóa mềm", items));
    }

    /**
     * Khôi phục một Ví tài chính bị xóa nhầm
     */
    @PostMapping("/wallets/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreWallet(@PathVariable Long id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        recoveryService.restoreWallet(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục ví tài chính thành công", null));
    }

    /**
     * Khôi phục một Giao dịch bị xóa nhầm và đồng bộ lại số dư
     */
    @PostMapping("/transactions/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreTransaction(@PathVariable Long id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        recoveryService.restoreTransaction(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục giao dịch thành công", null));
    }

    /**
     * Khôi phục một Ngân sách bị xóa nhầm
     */
    @PostMapping("/budgets/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreBudget(@PathVariable Long id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        recoveryService.restoreBudget(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục ngân sách thành công", null));
    }

    /**
     * Khôi phục một Quỹ mục tiêu tiết kiệm bị xóa nhầm
     */
    @PostMapping("/saving-goals/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreSavingGoal(@PathVariable Long id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        recoveryService.restoreSavingGoal(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Khôi phục mục tiêu tiết kiệm thành công", null));
    }
}
