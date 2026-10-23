package com.finvault.module3_budget_goals.entity;

import com.finvault.module1_security_rbac.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculation Entity: Lưu trữ kết quả tính toán kế hoạch trả nợ vay ngân hàng
 *
 * Tính năng chính:
 * - Lưu trữ lịch sử tính toán khoản vay (Mua nhà, Mua xe, Vay kinh doanh)
 * - Ghi nhận phương pháp tính: Trả góp đều (EQUAL_INSTALLMENTS) hoặc Dư nợ giảm dần (REDUCING_BALANCE)
 * - Lưu trữ tổng lãi phải trả và tổng số tiền phải thanh toán
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tự động liên kết lịch thanh toán khoản vay thành các giao dịch định kỳ (Auto-recurring Loan Installment Transactions)
 * ==============================================================================
 */
@Entity
@Table(name = "loan_calculations")
@EntityListeners(AuditingEntityListener.class)
public class LoanCalculation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "loan_title", length = 100, nullable = false)
    private String loanTitle;

    @Column(name = "principal_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "annual_interest_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal annualInterestRate = BigDecimal.ZERO;

    @Column(name = "loan_term_months", nullable = false)
    private Integer loanTermMonths;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", length = 30, nullable = false)
    private LoanCalculationMethod calculationMethod;

    @Column(name = "monthly_payment", precision = 18, scale = 2, nullable = false)
    private BigDecimal monthlyPayment = BigDecimal.ZERO;

    @Column(name = "total_interest", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalInterest = BigDecimal.ZERO;

    @Column(name = "total_payable", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalPayable = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public LoanCalculation() {
    }

    public LoanCalculation(User user, String loanTitle, BigDecimal principalAmount,
                           BigDecimal annualInterestRate, Integer loanTermMonths,
                           LocalDate startDate, LoanCalculationMethod calculationMethod,
                           BigDecimal monthlyPayment, BigDecimal totalInterest,
                           BigDecimal totalPayable, String notes) {
        this.user = user;
        this.loanTitle = loanTitle;
        this.principalAmount = (principalAmount != null) ? principalAmount : BigDecimal.ZERO;
        this.annualInterestRate = (annualInterestRate != null) ? annualInterestRate : BigDecimal.ZERO;
        this.loanTermMonths = loanTermMonths;
        this.startDate = startDate;
        this.calculationMethod = calculationMethod;
        this.monthlyPayment = (monthlyPayment != null) ? monthlyPayment : BigDecimal.ZERO;
        this.totalInterest = (totalInterest != null) ? totalInterest : BigDecimal.ZERO;
        this.totalPayable = (totalPayable != null) ? totalPayable : BigDecimal.ZERO;
        this.notes = notes;
        this.isDeleted = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanCalculation that = (LoanCalculation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LoanCalculation{" +
                "id=" + id +
                ", loanTitle='" + loanTitle + '\'' +
                ", principalAmount=" + principalAmount +
                ", annualInterestRate=" + annualInterestRate +
                ", loanTermMonths=" + loanTermMonths +
                ", calculationMethod=" + calculationMethod +
                ", monthlyPayment=" + monthlyPayment +
                '}';
    }
}
