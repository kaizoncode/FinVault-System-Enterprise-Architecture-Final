package com.finvault.module3_budget_goals.dto;

import com.finvault.module3_budget_goals.entity.LoanCalculation;
import com.finvault.module3_budget_goals.entity.LoanCalculationMethod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculationResponseDto: DTO phản hồi kết quả tính toán khoản vay và bảng lịch trình trả góp
 * ==============================================================================
 */
public class LoanCalculationResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String loanTitle;
    private BigDecimal principalAmount;
    private BigDecimal annualInterestRate;
    private Integer loanTermMonths;
    private LocalDate startDate;
    private LoanCalculationMethod calculationMethod;
    private BigDecimal monthlyPayment;
    private BigDecimal totalInterest;
    private BigDecimal totalPayable;
    private String notes;
    private LocalDateTime createdAt;
    private List<LoanAmortizationScheduleDto> amortizationSchedule = new ArrayList<>();

    public LoanCalculationResponseDto() {
    }

    public static LoanCalculationResponseDto fromEntity(LoanCalculation entity) {
        if (entity == null) {
            return null;
        }
        LoanCalculationResponseDto dto = new LoanCalculationResponseDto();
        dto.setId(entity.getId());
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUsername(entity.getUser().getUsername());
        }
        dto.setLoanTitle(entity.getLoanTitle());
        dto.setPrincipalAmount(entity.getPrincipalAmount());
        dto.setAnnualInterestRate(entity.getAnnualInterestRate());
        dto.setLoanTermMonths(entity.getLoanTermMonths());
        dto.setStartDate(entity.getStartDate());
        dto.setCalculationMethod(entity.getCalculationMethod());
        dto.setMonthlyPayment(entity.getMonthlyPayment());
        dto.setTotalInterest(entity.getTotalInterest());
        dto.setTotalPayable(entity.getTotalPayable());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLoanTitle() {
        return loanTitle;
    }

    public void setLoanTitle(String loanTitle) {
        this.loanTitle = loanTitle;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public Integer getLoanTermMonths() {
        return loanTermMonths;
    }

    public void setLoanTermMonths(Integer loanTermMonths) {
        this.loanTermMonths = loanTermMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LoanCalculationMethod getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(LoanCalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getTotalPayable() {
        return totalPayable;
    }

    public void setTotalPayable(BigDecimal totalPayable) {
        this.totalPayable = totalPayable;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<LoanAmortizationScheduleDto> getAmortizationSchedule() {
        return amortizationSchedule;
    }

    public void setAmortizationSchedule(List<LoanAmortizationScheduleDto> amortizationSchedule) {
        this.amortizationSchedule = amortizationSchedule;
    }
}
