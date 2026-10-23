package com.finvault.module3_budget_goals.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module3_budget_goals.dto.LoanCalculationRequest;
import com.finvault.module3_budget_goals.dto.LoanCalculationResponseDto;
import com.finvault.module3_budget_goals.service.LoanCalculatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculatorApiController: REST API Công cụ tính toán kế hoạch trả nợ vay ngân hàng
 *
 * Tính năng chính:
 * - Tính toán lịch trình trả góp chi tiết theo Niên kim cố định (EQUAL_INSTALLMENTS)
 *   hoặc Dư nợ giảm dần (REDUCING_BALANCE)
 * - Tự động lưu lịch sử tính toán cho người dùng đã đăng nhập
 * - Tra cứu và quản lý lịch sử các phương án vay vốn đã lập
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/loans")
public class LoanCalculatorApiController {

    private final LoanCalculatorService loanCalculatorService;

    public LoanCalculatorApiController(LoanCalculatorService loanCalculatorService) {
        this.loanCalculatorService = loanCalculatorService;
    }

    /**
     * Tính toán kế hoạch trả nợ vay ngân hàng (Hỗ trợ cả người dùng ẩn danh và đã đăng nhập)
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<LoanCalculationResponseDto>> calculateLoan(
            @Valid @RequestBody LoanCalculationRequest request) {
        String username = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            username = authentication.getName();
        }

        LoanCalculationResponseDto response = loanCalculatorService.calculateLoan(username, request);
        return ResponseEntity.ok(ApiResponse.success("Tính toán lịch trình trả nợ thành công", response));
    }

    /**
     * Lấy lịch sử các phương án tính toán khoản vay đã lưu của người dùng
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<LoanCalculationResponseDto>>> getLoanHistory() {
        String username = SecurityUtils.getCurrentUsername();
        List<LoanCalculationResponseDto> history = loanCalculatorService.getUserLoanCalculations(username);
        return ResponseEntity.ok(ApiResponse.success("Lịch sử tính toán khoản vay của bạn", history));
    }

    /**
     * Tra cứu chi tiết một bản ghi tính toán khoản vay theo ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LoanCalculationResponseDto>> getLoanCalculationById(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        LoanCalculationResponseDto response = loanCalculatorService.getLoanCalculationById(id, username);
        return ResponseEntity.ok(ApiResponse.success("Chi tiết phương án vay vốn", response));
    }

    /**
     * Xóa mềm một bản ghi lịch sử tính toán khoản vay
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteLoanCalculation(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        loanCalculatorService.deleteLoanCalculation(id, username);
        return ResponseEntity.ok(ApiResponse.success("Xóa lịch sử tính toán thành công", null));
    }
}
