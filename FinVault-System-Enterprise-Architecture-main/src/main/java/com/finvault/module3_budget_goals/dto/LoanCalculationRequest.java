package com.finvault.module3_budget_goals.dto;

import com.finvault.module3_budget_goals.entity.LoanCalculationMethod;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculationRequest: DTO yêu cầu tính toán lịch trình trả nợ khoản vay ngân hàng
 * ==============================================================================
 */
public class LoanCalculationRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên khoản vay không được để trống")
    @Size(max = 100, message = "Tên khoản vay tối đa 100 ký tự")
    private String loanTitle;

    @NotNull(message = "Số tiền gốc vay không được để trống")
    @DecimalMin(value = "1000000.00", message = "Số tiền vay tối thiểu là 1,000,000 VND")
    private BigDecimal principalAmount;

    @NotNull(message = "Lãi suất năm không được để trống")
    @DecimalMin(value = "0.01", message = "Lãi suất tối thiểu là 0.01% / năm")
    @DecimalMax(value = "100.00", message = "Lãi suất tối đa là 100% / năm")
    private BigDecimal annualInterestRate;

    @NotNull(message = "Thời hạn vay không được để trống")
    @Min(value = 1, message = "Thời hạn vay tối thiểu là 1 tháng")
    @Max(value = 480, message = "Thời hạn vay tối đa là 480 tháng (40 năm)")
    private Integer loanTermMonths;

    @NotNull(message = "Ngày giải ngân / bắt đầu vay không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Phương thức tính toán không được để trống")
    private LoanCalculationMethod calculationMethod;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String notes;

    private Boolean saveHistory = true;

    public LoanCalculationRequest() {
    }

    public LoanCalculationRequest(String loanTitle, BigDecimal principalAmount,
                                  BigDecimal annualInterestRate, Integer loanTermMonths,
                                  LocalDate startDate, LoanCalculationMethod calculationMethod,
                                  String notes, Boolean saveHistory) {
        this.loanTitle = loanTitle;
        this.principalAmount = principalAmount;
        this.annualInterestRate = annualInterestRate;
        this.loanTermMonths = loanTermMonths;
        this.startDate = startDate;
        this.calculationMethod = calculationMethod;
        this.notes = notes;
        this.saveHistory = (saveHistory != null) ? saveHistory : true;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getSaveHistory() {
        return saveHistory;
    }

    public void setSaveHistory(Boolean saveHistory) {
        this.saveHistory = saveHistory;
    }
}
