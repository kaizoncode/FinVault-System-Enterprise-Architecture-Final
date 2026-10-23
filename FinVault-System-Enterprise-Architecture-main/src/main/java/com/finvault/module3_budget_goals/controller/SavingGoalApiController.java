package com.finvault.module3_budget_goals.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module3_budget_goals.dto.SavingGoalDepositRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalResponseDto;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;
import com.finvault.module3_budget_goals.service.SavingGoalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalApiController: REST API Quản trị Quỹ mục tiêu tiết kiệm
 *
 * Tính năng chính:
 * - CRUD Quỹ mục tiêu tiết kiệm tài chính (Mua nhà, Mua xe, Quỹ khẩn cấp)
 * - Nạp tiền vào quỹ từ Ví cá nhân với cơ chế khóa bi quan (Pessimistic Lock) chống Race Condition
 * - Rút tiền từ quỹ mục tiêu về ví khi cần thanh toán hoặc hoàn thành mục tiêu
 * - Theo dõi tiến độ tích lũy và lọc theo trạng thái (IN_PROGRESS, ACHIEVED, CANCELLED)
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/saving-goals")
@PreAuthorize("isAuthenticated()")
public class SavingGoalApiController {

    private final SavingGoalService savingGoalService;

    public SavingGoalApiController(SavingGoalService savingGoalService) {
        this.savingGoalService = savingGoalService;
    }

    /**
     * Khởi tạo Quỹ mục tiêu tiết kiệm mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SavingGoalResponseDto>> createGoal(@Valid @RequestBody SavingGoalRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        SavingGoalResponseDto response = savingGoalService.createGoal(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo mục tiêu tiết kiệm thành công", response));
    }

    /**
     * Cập nhật thông tin Quỹ mục tiêu tiết kiệm
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingGoalResponseDto>> updateGoal(@PathVariable("id") Long id,
                                                                         @Valid @RequestBody SavingGoalRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        SavingGoalResponseDto response = savingGoalService.updateGoal(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mục tiêu tiết kiệm thành công", response));
    }

    /**
     * Hủy / Xóa mềm Quỹ mục tiêu tiết kiệm
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        savingGoalService.deleteGoal(id, username);
        return ResponseEntity.ok(ApiResponse.success("Hủy mục tiêu tiết kiệm thành công", null));
    }

    /**
     * Xem thông tin chi tiết một Quỹ mục tiêu tiết kiệm theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingGoalResponseDto>> getGoalById(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        SavingGoalResponseDto response = savingGoalService.getGoalById(id, username);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết mục tiêu tiết kiệm", response));
    }

    /**
     * Lấy toàn bộ danh sách Quỹ mục tiêu của người dùng, hỗ trợ lọc theo trạng thái
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SavingGoalResponseDto>>> getUserGoals(
            @RequestParam(value = "status", required = false) SavingGoalStatus status) {
        String username = SecurityUtils.getCurrentUsername();
        List<SavingGoalResponseDto> goals = (status != null)
                ? savingGoalService.getGoalsByStatus(username, status)
                : savingGoalService.getUserGoals(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách mục tiêu tiết kiệm", goals));
    }

    /**
     * Nạp tiền tích lũy từ ví vào Quỹ mục tiêu tiết kiệm (Đảm bảo ACID & Chống Race Condition)
     */
    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<SavingGoalResponseDto>> depositToGoal(
            @PathVariable("id") Long id,
            @Valid @RequestBody SavingGoalDepositRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        SavingGoalResponseDto response = savingGoalService.depositToGoal(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Nạp tiền vào mục tiêu tiết kiệm thành công", response));
    }

    /**
     * Rút tiền từ Quỹ mục tiêu tiết kiệm (Có thể tùy chọn chuyển về ví đích)
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<SavingGoalResponseDto>> withdrawFromGoal(
            @PathVariable("id") Long id,
            @RequestParam("amount") @DecimalMin(value = "1000.00", message = "Số tiền rút tối thiểu là 1,000 VND") BigDecimal amount,
            @RequestParam(value = "targetWalletId", required = false) Long targetWalletId) {
        String username = SecurityUtils.getCurrentUsername();
        SavingGoalResponseDto response = savingGoalService.withdrawFromGoal(id, username, amount, targetWalletId);
        return ResponseEntity.ok(ApiResponse.success("Rút tiền từ mục tiêu tiết kiệm thành công", response));
    }
}
