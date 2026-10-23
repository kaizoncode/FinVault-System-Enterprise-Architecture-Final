package com.finvault.module3_budget_goals.entity;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoal Entity: Thực thể Quỹ mục tiêu tiết kiệm
 *
 * Tính năng chính:
 * - Thiết lập mục tiêu tài chính (Mua nhà, Mua xe, Quỹ khẩn cấp, Du lịch)
 * - Tích hợp nạp tiền từ Ví cá nhân vào Quỹ mục tiêu
 * - Theo dõi tiến độ phần trăm đạt được và dự báo hoàn thành
 * - Hỗ trợ Optimistic Locking (@Version) và Soft Delete (isDeleted)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tự động trích tiền định kỳ từ ví vào quỹ (Auto-saving round-up / recurring deposit)
 * ==============================================================================
 */
@Entity
@Table(name = "saving_goals")
@EntityListeners(AuditingEntityListener.class)
public class SavingGoal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(name = "goal_name", length = 100, nullable = false)
    private String goalName;

    @Column(name = "target_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal targetAmount = BigDecimal.ZERO;

    @Column(name = "current_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private SavingGoalStatus status = SavingGoalStatus.IN_PROGRESS;

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

    @Version
    @Column(name = "version")
    private Long version = 0L;

    public SavingGoal() {
    }

    public SavingGoal(User user, Wallet wallet, String goalName, BigDecimal targetAmount,
                      BigDecimal initialAmount, LocalDate targetDate, String notes) {
        this.user = user;
        this.wallet = wallet;
        this.goalName = goalName;
        this.targetAmount = (targetAmount != null) ? targetAmount : BigDecimal.ZERO;
        this.currentAmount = (initialAmount != null) ? initialAmount : BigDecimal.ZERO;
        this.targetDate = targetDate;
        this.notes = notes;
        this.status = (this.currentAmount.compareTo(this.targetAmount) >= 0)
                ? SavingGoalStatus.ACHIEVED
                : SavingGoalStatus.IN_PROGRESS;
        this.isDeleted = false;
    }

    /**
     * Nạp tiền tiết kiệm vào quỹ mục tiêu
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp vào mục tiêu tiết kiệm phải lớn hơn 0");
        }
        this.currentAmount = this.currentAmount.add(amount);
        if (this.currentAmount.compareTo(this.targetAmount) >= 0) {
            this.status = SavingGoalStatus.ACHIEVED;
        }
    }

    /**
     * Rút tiền từ quỹ mục tiêu tiết kiệm
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền rút phải lớn hơn 0");
        }
        if (this.currentAmount.compareTo(amount) < 0) {
            throw new BusinessException("INSUFFICIENT_GOAL_FUNDS", "Số dư tích lũy hiện tại không đủ để rút");
        }
        this.currentAmount = this.currentAmount.subtract(amount);
        if (this.currentAmount.compareTo(this.targetAmount) < 0 && this.status == SavingGoalStatus.ACHIEVED) {
            this.status = SavingGoalStatus.IN_PROGRESS;
        }
    }

    /**
     * Tính toán phần trăm tiến độ đã đạt được
     */
    public BigDecimal getProgressPercentage() {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_UP);
    }

    /**
     * Tính toán số tiền còn thiếu để hoàn thành mục tiêu
     */
    public BigDecimal getRemainingAmount() {
        if (currentAmount.compareTo(targetAmount) >= 0) {
            return BigDecimal.ZERO;
        }
        return targetAmount.subtract(currentAmount);
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

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public SavingGoalStatus getStatus() {
        return status;
    }

    public void setStatus(SavingGoalStatus status) {
        this.status = status;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavingGoal that = (SavingGoal) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SavingGoal{" +
                "id=" + id +
                ", goalName='" + goalName + '\'' +
                ", targetAmount=" + targetAmount +
                ", currentAmount=" + currentAmount +
                ", targetDate=" + targetDate +
                ", status=" + status +
                '}';
    }
}
