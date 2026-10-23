package com.finvault.module3_budget_goals.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * Budget50_30_20Request: DTO yêu cầu khởi tạo bộ 3 ngân sách tự động theo Quy tắc 50/30/20
 *
 * Thuật toán phân bổ:
 * - 50% Nhu cầu thiết yếu (Needs): Ăn uống, Nhà cửa, Hóa đơn điện nước
 * - 30% Sở thích cá nhân (Wants): Mua sắm, Giải trí, Ăn ngoài
 * - 20% Tích lũy tiết kiệm (Savings): Tích lũy quỹ khẩn cấp, Trả nợ hoặc Đầu tư
 * ==============================================================================
 */
public class Budget50_30_20Request implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "Tổng thu nhập tháng không được để trống")
    @DecimalMin(value = "1000000.00", message = "Thu nhập tối thiểu để áp dụng quy tắc 50/30/20 là 1,000,000 VND")
    private BigDecimal monthlyIncome;

    private Long walletId;

    @NotNull(message = "Ngày bắt đầu chu kỳ không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc chu kỳ không được để trống")
    private LocalDate endDate;

    public Budget50_30_20Request() {
    }

    public Budget50_30_20Request(BigDecimal monthlyIncome, Long walletId, LocalDate startDate, LocalDate endDate) {
        this.monthlyIncome = monthlyIncome;
        this.walletId = walletId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
