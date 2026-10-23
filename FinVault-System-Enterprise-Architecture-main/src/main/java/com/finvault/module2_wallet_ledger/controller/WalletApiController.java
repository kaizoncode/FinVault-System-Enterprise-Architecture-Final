package com.finvault.module2_wallet_ledger.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module2_wallet_ledger.dto.WalletMemberDto;
import com.finvault.module2_wallet_ledger.dto.WalletRequest;
import com.finvault.module2_wallet_ledger.dto.WalletResponseDto;
import com.finvault.module2_wallet_ledger.service.WalletService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletApiController: REST API quản trị Đa Ví và Quỹ tài chính gia đình
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/wallets")
@PreAuthorize("isAuthenticated()")
public class WalletApiController {

    private final WalletService walletService;

    public WalletApiController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * Lấy toàn bộ danh sách ví khả dụng của người dùng (Cá nhân + Gia đình)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WalletResponseDto>>> getUserWallets() {
        String username = SecurityUtils.getCurrentUsername();
        List<WalletResponseDto> wallets = walletService.getUserWallets(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách ví tài chính", wallets));
    }

    /**
     * Lấy danh sách Quỹ chung gia đình mà người dùng tham gia hoặc làm chủ
     */
    @GetMapping("/family")
    public ResponseEntity<ApiResponse<List<WalletResponseDto>>> getFamilyWallets() {
        String username = SecurityUtils.getCurrentUsername();
        List<WalletResponseDto> familyWallets = walletService.getFamilyWallets(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách quỹ gia đình", familyWallets));
    }

    /**
     * Xem thông tin chi tiết một ví theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> getWalletById(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto wallet = walletService.getWalletById(id, username);
        return ResponseEntity.ok(ApiResponse.success("Thông tin chi tiết ví", wallet));
    }

    /**
     * Khởi tạo một ví mới (Áp dụng WalletFactory)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WalletResponseDto>> createWallet(@Valid @RequestBody WalletRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto created = walletService.createWallet(username, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo ví mới thành công!", created));
    }

    /**
     * Cập nhật thông tin cấu hình ví
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> updateWallet(
            @PathVariable("id") Long id,
            @Valid @RequestBody WalletRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto updated = walletService.updateWallet(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ví thành công", updated));
    }

    /**
     * Xóa ví (Soft Delete) khi số dư đã về 0
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWallet(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        walletService.deleteWallet(id, username);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa ví thành công", null));
    }

    /**
     * [Dành cho Gia trưởng / Chủ quỹ]: Thêm thành viên vào Quỹ gia đình
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<WalletResponseDto>> addFamilyMember(
            @PathVariable("id") Long id,
            @Valid @RequestBody WalletMemberDto dto) {
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto updated = walletService.addFamilyMember(id, username, dto);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành viên vào quỹ gia đình thành công!", updated));
    }

    /**
     * [Dành cho Gia trưởng / Chủ quỹ]: Xóa thành viên khỏi Quỹ gia đình
     */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<WalletResponseDto>> removeFamilyMember(
            @PathVariable("id") Long id,
            @PathVariable("userId") Long userId) {
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto updated = walletService.removeFamilyMember(id, username, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thành viên khỏi quỹ gia đình", updated));
    }

    /**
     * [Dành cho Gia trưởng / Chủ quỹ]: Điều chỉnh hạn mức chi tiêu cho thành viên
     */
    @RequestMapping(value = "/{id}/members/{userId}/limit", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<WalletResponseDto>> updateMemberSpendingLimit(
            @PathVariable("id") Long id,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "limit", required = false) @DecimalMin(value = "0.00", message = "Hạn mức chi tiêu phải lớn hơn hoặc bằng 0") BigDecimal limit,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        BigDecimal effectiveLimit = limit;
        if (effectiveLimit == null && body != null) {
            Object raw = body.getOrDefault("limit", body.get("spendingLimit"));
            if (raw != null) {
                effectiveLimit = new BigDecimal(raw.toString());
            }
        }
        if (effectiveLimit == null) {
            effectiveLimit = BigDecimal.ZERO;
        }
        String username = SecurityUtils.getCurrentUsername();
        WalletResponseDto updated = walletService.updateMemberSpendingLimit(id, username, userId, effectiveLimit);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hạn mức chi thành viên thành công", updated));
    }
}
