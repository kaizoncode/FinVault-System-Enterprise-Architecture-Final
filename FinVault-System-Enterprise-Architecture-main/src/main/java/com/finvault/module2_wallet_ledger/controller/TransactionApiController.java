package com.finvault.module2_wallet_ledger.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module2_wallet_ledger.dto.*;
import com.finvault.module2_wallet_ledger.service.ReconciliationService;
import com.finvault.module2_wallet_ledger.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * TransactionApiController: REST API điều phối Lõi Giao dịch & Đối soát Sổ cái
 *
 * Tính năng chính:
 * - Ghi nhận thu, chi có bảo vệ ACID và chống Race Condition
 * - Chuyển tiền liên ví chống Deadlock
 * - Phê duyệt / Từ chối khoản chi lớn của gia đình (>= 5,000,000 VND)
 * - Đối soát số dư toàn vẹn (Ledger Reconciliation)
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/transactions")
@PreAuthorize("isAuthenticated()")
public class TransactionApiController {

    private final TransactionService transactionService;
    private final ReconciliationService reconciliationService;

    public TransactionApiController(
            TransactionService transactionService,
            ReconciliationService reconciliationService) {
        this.transactionService = transactionService;
        this.reconciliationService = reconciliationService;
    }

    /**
     * Ghi nhận giao dịch Thu / Chi mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponseDto>> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        TransactionResponseDto result = transactionService.createTransaction(username, request);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận giao dịch thành công", result));
    }

    /**
     * Cập nhật giao dịch Thu / Chi và đồng bộ lại số dư ví & ngân sách
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> updateTransaction(
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        TransactionResponseDto result = transactionService.updateTransaction(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giao dịch thành công", result));
    }

    /**
     * Xóa (Soft delete) giao dịch và hoàn trả số dư ví an toàn
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        transactionService.deleteTransaction(id, username);
        return ResponseEntity.ok(ApiResponse.success("Xóa giao dịch thành công", null));
    }

    /**
     * Chuyển tiền liên ví (Inter-wallet Transfer) chống Deadlock và Race Condition
     */
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> transferFunds(
            @Valid @RequestBody TransferRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        TransactionResponseDto result = transactionService.transferFunds(username, request);
        return ResponseEntity.ok(ApiResponse.success("Chuyển tiền liên ví thành công", result));
    }

    /**
     * Xem lịch sử giao dịch của một ví cụ thể (có phân trang)
     */
    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getWalletTransactions(
            @PathVariable("walletId") Long walletId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        String username = SecurityUtils.getCurrentUsername();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<TransactionResponseDto> transactions = transactionService.getTransactionsByWallet(walletId, username, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Lịch sử giao dịch của ví", transactions));
    }

    /**
     * Xem toàn bộ lịch sử giao dịch của bản thân (có phân trang)
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TransactionResponseDto>>> getMyTransactions(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        String username = SecurityUtils.getCurrentUsername();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<TransactionResponseDto> transactions = transactionService.getUserTransactions(username, pageRequest);
        return ResponseEntity.ok(ApiResponse.success("Lịch sử giao dịch cá nhân", transactions));
    }

    /**
     * [Dành cho Gia trưởng & Super Admin]: Lấy danh sách giao dịch gia đình đang chờ duyệt (PENDING)
     */
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FAMILY_HEAD')")
    public ResponseEntity<ApiResponse<List<TransactionResponseDto>>> getPendingApprovals() {
        String username = SecurityUtils.getCurrentUsername();
        List<TransactionResponseDto> pendings = transactionService.getPendingFamilyTransactions(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách giao dịch chờ phê duyệt", pendings));
    }

    /**
     * [Dành cho Gia trưởng & Super Admin]: Phê duyệt hoặc Từ chối khoản chi lớn gia đình
     */
    @PostMapping("/{id}/approval")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'FAMILY_HEAD')")
    public ResponseEntity<ApiResponse<TransactionResponseDto>> decideTransactionApproval(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApprovalDecisionDto decision) {
        String username = SecurityUtils.getCurrentUsername();
        TransactionResponseDto result = transactionService.approveOrRejectTransaction(id, username, decision);
        String msg = Boolean.TRUE.equals(decision.getApproved())
                ? "Đã phê duyệt giao dịch thành công"
                : "Đã từ chối giao dịch thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    /**
     * Đối soát tính toàn vẹn số dư của một ví (So sánh số dư thực tế vs Net Flow)
     */
    @GetMapping("/reconcile/{walletId}")
    public ResponseEntity<ApiResponse<ReconciliationReportDto>> reconcileWallet(@PathVariable("walletId") Long walletId) {
        String username = SecurityUtils.getCurrentUsername();
        ReconciliationReportDto report = reconciliationService.reconcileWallet(walletId, username);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo đối soát số dư ví", report));
    }

    /**
     * [Dành cho Auditor & Super Admin]: Đối soát toàn bộ số dư tất cả các ví trong hệ thống
     */
    @GetMapping("/reconcile-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<ApiResponse<List<ReconciliationReportDto>>> reconcileAllWallets() {
        String username = SecurityUtils.getCurrentUsername();
        List<ReconciliationReportDto> reports = reconciliationService.reconcileAllWallets(username);
        return ResponseEntity.ok(ApiResponse.success("Báo cáo đối soát toàn bộ ví hệ thống", reports));
    }
}
