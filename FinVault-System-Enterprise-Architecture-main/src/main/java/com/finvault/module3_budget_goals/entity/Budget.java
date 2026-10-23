package com.finvault.module3_budget_goals.entity;

import com.finvault.common.BaseEntity;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * Budget Entity: Thực thể Ngân sách tài chính thông minh
 *
 * Tính năng kỹ thuật nâng cao:
 * - Kế thừa BaseEntity: Tự động Auditing, Soft Delete và Optimistic Locking (@Version)
 * - Quản lý ngân sách theo Quy tắc 50/30/20 (Needs, Wants, Savings) và tùy chỉnh
 * - Theo dõi chi tiêu thực tế (spentAmount) và tỷ lệ sử dụng (usagePercentage)
 * - Tích hợp các cờ trạng thái cảnh báo tự động: alertThreshold80Sent, alertThreshold100Sent
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ Smart Auto-Rollover (tự động chuyển số dư ngân sách còn dư sang tháng sau)
 * ==============================================================================
 */
@Entity
@Table(name = "budgets")
public class Budget extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(name = "budget_name", length = 100, nullable = false)
    private String budgetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_type", length = 30)
    private BudgetType budgetType = BudgetType.CUSTOM;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", length = 30)
    private AllocationType allocationType = AllocationType.NEEDS;

    @Column(name = "allocated_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "spent_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal spentAmount = BigDecimal.ZERO;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "alert_threshold_80_sent")
    private Boolean alertThreshold80Sent = false;

    @Column(name = "alert_threshold_100_sent")
    private Boolean alertThreshold100Sent = false;

    @Column(name = "original_duration")
    private Long originalDuration;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Budget() {
    }

    public Budget(User user, Category category, Wallet wallet, String budgetName,
                  BudgetType budgetType, AllocationType allocationType,
                  BigDecimal allocatedAmount, LocalDate startDate, LocalDate endDate) {
        this.user = user;
        this.category = category;
        this.wallet = wallet;
        this.budgetName = budgetName;
        this.budgetType = (budgetType != null) ? budgetType : BudgetType.CUSTOM;
        this.allocationType = (allocationType != null) ? allocationType : AllocationType.NEEDS;
        this.allocatedAmount = (allocatedAmount != null) ? allocatedAmount : BigDecimal.ZERO;
        this.spentAmount = BigDecimal.ZERO;
        this.startDate = startDate;
        this.endDate = endDate;
        if (startDate != null && endDate != null) {
            this.originalDuration = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        this.alertThreshold80Sent = false;
        this.alertThreshold100Sent = false;
        this.isActive = true;
    }

    /**
     * Tính toán số tiền ngân sách còn lại
     */
    public BigDecimal getRemainingAmount() {
        return allocatedAmount.subtract(spentAmount);
    }

    /**
     * Tính toán phần trăm ngân sách đã sử dụng (Ví dụ: 85.50%)
     */
    public BigDecimal getUsagePercentage() {
        if (allocatedAmount == null || allocatedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return spentAmount.multiply(BigDecimal.valueOf(100))
                .divide(allocatedAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra xem ngân sách đã vượt ngưỡng (>= 100%) hay chưa
     */
    public boolean isOverBudget() {
        return spentAmount.compareTo(allocatedAmount) >= 0;
    }

    /**
     * Ghi nhận chi tiêu thêm vào ngân sách
     */
    public void recordExpense(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.spentAmount = this.spentAmount.add(amount);
        }
    }

    /**
     * Giảm số tiền chi tiêu khi giao dịch bị xóa hoặc hoàn tác
     */
    public void revertExpense(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.spentAmount = this.spentAmount.subtract(amount);
            if (this.spentAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.spentAmount = BigDecimal.ZERO;
            }
        }
    }

    // Getters and Setters
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public String getBudgetName() {
        return budgetName;
    }

    public void setBudgetName(String budgetName) {
        this.budgetName = budgetName;
    }

    public BudgetType getBudgetType() {
        return budgetType;
    }

    public void setBudgetType(BudgetType budgetType) {
        this.budgetType = budgetType;
    }

    public AllocationType getAllocationType() {
        return allocationType;
    }

    public void setAllocationType(AllocationType allocationType) {
        this.allocationType = allocationType;
    }

    public BigDecimal getAllocatedAmount() {
        return allocatedAmount;
    }

    public void setAllocatedAmount(BigDecimal allocatedAmount) {
        this.allocatedAmount = allocatedAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount;
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

    public Boolean getAlertThreshold80Sent() {
        return alertThreshold80Sent;
    }

    public void setAlertThreshold80Sent(Boolean alertThreshold80Sent) {
        this.alertThreshold80Sent = alertThreshold80Sent;
    }

    public Boolean getAlertThreshold100Sent() {
        return alertThreshold100Sent;
    }

    public void setAlertThreshold100Sent(Boolean alertThreshold100Sent) {
        this.alertThreshold100Sent = alertThreshold100Sent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Long getOriginalDuration() {
        if (originalDuration != null) {
            return originalDuration;
        }
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return null;
    }

    public void setOriginalDuration(Long originalDuration) {
        this.originalDuration = originalDuration;
    }

    public Long getOriginalDurationDays() {
        return getOriginalDuration();
    }

    public void setOriginalDurationDays(Long originalDurationDays) {
        this.originalDuration = originalDurationDays;
    }

    @PrePersist
    @PreUpdate
    public void calculateOriginalDuration() {
        if (this.originalDuration == null && this.startDate != null && this.endDate != null) {
            this.originalDuration = java.time.temporal.ChronoUnit.DAYS.between(this.startDate, this.endDate) + 1;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Budget budget = (Budget) o;
        return Objects.equals(getId(), budget.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public String toString() {
        return "Budget{" +
                "id=" + getId() +
                ", budgetName='" + budgetName + '\'' +
                ", budgetType=" + budgetType +
                ", allocationType=" + allocationType +
                ", allocatedAmount=" + allocatedAmount +
                ", spentAmount=" + spentAmount +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isActive=" + isActive +
                '}';
    }
}
