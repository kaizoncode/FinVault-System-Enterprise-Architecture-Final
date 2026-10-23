package com.finvault.module3_budget_goals.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module3_budget_goals.dto.Budget50_30_20Request;
import com.finvault.module3_budget_goals.dto.BudgetRequest;
import com.finvault.module3_budget_goals.dto.BudgetResponseDto;
import com.finvault.module3_budget_goals.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetApiController: REST API Quản trị Ngân sách tài chính thông minh
 *
 * Tính năng chính:
 * - CRUD Ngân sách cá nhân và danh mục chi tiêu
 * - Tạo tự động bộ 3 ngân sách theo Quy tắc Tỷ lệ vàng 50/30/20
 * - Xem danh sách ngân sách đang hoạt động và theo dõi cảnh báo thời gian thực
 * - Quản trị và tra cứu danh sách mẫu ngân sách chuẩn của hệ thống (STANDARD_TEMPLATE)
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/budgets")
@PreAuthorize("isAuthenticated()")
public class BudgetApiController {

    private final BudgetService budgetService;

    public BudgetApiController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Khởi tạo ngân sách mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponseDto>> createBudget(@Valid @RequestBody BudgetRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        BudgetResponseDto response = budgetService.createBudget(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo ngân sách thành công", response));
    }

    /**
     * Tự động khởi tạo trọn gói 3 ngân sách theo Quy tắc 50/30/20 (Needs, Wants, Savings)
     */
    @PostMapping("/50-30-20")
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> create50_30_20Budgets(@Valid @RequestBody Budget50_30_20Request request) {
        String username = SecurityUtils.getCurrentUsername();
        List<BudgetResponseDto> response = budgetService.create50_30_20Budgets(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo bộ 3 ngân sách 50/30/20 thành công", response));
    }

    /**
     * Cập nhật thông tin hoặc hạn mức ngân sách
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> updateBudget(@PathVariable("id") Long id,
                                                                       @Valid @RequestBody BudgetRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        BudgetResponseDto response = budgetService.updateBudget(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ngân sách thành công", response));
    }

    /**
     * Xóa mềm một ngân sách
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        budgetService.deleteBudget(id, username);
        return ResponseEntity.ok(ApiResponse.success("Xóa ngân sách thành công", null));
    }

    /**
     * Tra cứu chi tiết một ngân sách theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> getBudgetById(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        BudgetResponseDto response = budgetService.getBudgetById(id, username);
        return ResponseEntity.ok(ApiResponse.success("Thông tin chi tiết ngân sách", response));
    }

    /**
     * Lấy toàn bộ danh sách ngân sách của người dùng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getUserBudgets() {
        String username = SecurityUtils.getCurrentUsername();
        List<BudgetResponseDto> budgets = budgetService.getUserBudgets(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách ngân sách của bạn", budgets));
    }

    /**
     * Lấy các ngân sách đang có hiệu lực trong ngày hôm nay
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getActiveBudgets() {
        String username = SecurityUtils.getCurrentUsername();
        List<BudgetResponseDto> activeBudgets = budgetService.getActiveBudgets(username);
        return ResponseEntity.ok(ApiResponse.success("Danh sách ngân sách đang có hiệu lực", activeBudgets));
    }

    /**
     * Tra cứu danh sách các mẫu ngân sách chuẩn (STANDARD_TEMPLATE)
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<BudgetResponseDto>>> getStandardTemplates() {
        List<BudgetResponseDto> templates = budgetService.getStandardTemplates();
        return ResponseEntity.ok(ApiResponse.success("Danh sách mẫu ngân sách chuẩn hệ thống", templates));
    }

    /**
     * Quản trị viên (Admin) tạo mới một mẫu ngân sách chuẩn cho toàn hệ thống
     */
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BudgetResponseDto>> createStandardTemplate(@Valid @RequestBody BudgetRequest request) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        BudgetResponseDto template = budgetService.createStandardTemplate(adminUsername, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo mẫu ngân sách chuẩn thành công", template));
    }
}
