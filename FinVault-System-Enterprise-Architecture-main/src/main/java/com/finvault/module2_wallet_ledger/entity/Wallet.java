package com.finvault.module2_wallet_ledger.entity;

import com.finvault.common.BaseEntity;
import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * Wallet Entity: Thực thể Ví tài chính đa mục đích
 *
 * Tính năng kỹ thuật cốt lõi:
 * - Kế thừa BaseEntity: Tự động Auditing, Soft Delete và Optimistic Locking (@Version)
 * - Quản lý số dư chính xác cao với BigDecimal
 * - Phân loại 4 hình thức ví: CASH, BANK, CREDIT_CARD, FAMILY_FUND
 * - Cơ chế bảo vệ số dư: Kiểm tra hạn mức chi và số dư khả dụng
 * - Hỗ trợ liên kết thành viên gia đình (WalletMember)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ ví tiền tệ đa quốc gia (Multi-currency Wallet với tỷ giá hối đoái FX real-time)
 * ==============================================================================
 */
@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "wallet_name", nullable = false, length = 100)
    private String walletName;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 30)
    private WalletType walletType;

    @Column(name = "balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "initial_balance", precision = 18, scale = 2)
    private BigDecimal initialBalance = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "VND";

    @Column(name = "bank_account_number", length = 255)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "is_family_wallet", nullable = false)
    private Boolean isFamilyWallet = false;

    @Column(name = "monthly_spending_limit", precision = 18, scale = 2)
    private BigDecimal monthlySpendingLimit = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<WalletMember> members = new HashSet<>();

    public Wallet() {
    }

    public Wallet(User user, String walletName, WalletType walletType, BigDecimal initialBalance, String currency) {
        this.user = user;
        this.walletName = walletName;
        this.walletType = walletType;
        this.initialBalance = (initialBalance != null) ? initialBalance : BigDecimal.ZERO;
        this.balance = this.initialBalance;
        this.currency = (currency != null && !currency.isBlank()) ? currency : "VND";
        this.isFamilyWallet = (walletType == WalletType.FAMILY_FUND);
        this.isActive = true;
    }

    /**
     * Nạp tiền vào ví
     */
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp phải lớn hơn 0");
        }
        this.balance = this.balance.add(amount);
    }

    /**
     * Rút tiền / Trừ tiền từ ví (Có kiểm tra số dư khả dụng và hạn mức thẻ tín dụng)
     */
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền chi tiêu phải lớn hơn 0");
        }
        // Thẻ tín dụng cho phép âm tiền trong hạn mức tín dụng (credit limit)
        if (this.walletType == WalletType.CREDIT_CARD) {
            BigDecimal limit = (this.monthlySpendingLimit != null) ? this.monthlySpendingLimit : BigDecimal.ZERO;
            BigDecimal availableCredit = this.balance.add(limit);
            if (availableCredit.compareTo(amount) < 0) {
                throw new BusinessException("CREDIT_LIMIT_EXCEEDED",
                        String.format("Hạn mức tín dụng ví '%s' không đủ. Khả dụng: %s %s, cần: %s %s",
                                this.walletName, availableCredit.toPlainString(), this.currency,
                                amount.toPlainString(), this.currency));
            }
        } else if (this.balance.compareTo(amount) < 0) {
            // Các loại ví khác (CASH, BANK, FAMILY_FUND) bắt buộc balance >= amount
            throw new BusinessException("INSUFFICIENT_FUNDS",
                    String.format("Số dư ví '%s' không đủ. Hiện có: %s %s, cần: %s %s",
                            this.walletName, this.balance.toPlainString(), this.currency,
                            amount.toPlainString(), this.currency));
        }
        this.balance = this.balance.subtract(amount);
    }

    public void addMember(WalletMember member) {
        this.members.add(member);
        member.setWallet(this);
    }

    public void removeMember(WalletMember member) {
        this.members.remove(member);
        member.setWallet(null);
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public WalletType getWalletType() {
        return walletType;
    }

    public void setWalletType(WalletType walletType) {
        this.walletType = walletType;
        if (walletType == WalletType.FAMILY_FUND) {
            this.isFamilyWallet = true;
        }
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInitialBalance() {
        return (initialBalance != null) ? initialBalance : BigDecimal.ZERO;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = (initialBalance != null) ? initialBalance : BigDecimal.ZERO;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public Boolean getIsFamilyWallet() {
        return isFamilyWallet;
    }

    public void setIsFamilyWallet(Boolean familyWallet) {
        isFamilyWallet = familyWallet;
    }

    public BigDecimal getMonthlySpendingLimit() {
        return monthlySpendingLimit;
    }

    public void setMonthlySpendingLimit(BigDecimal monthlySpendingLimit) {
        this.monthlySpendingLimit = monthlySpendingLimit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Set<WalletMember> getMembers() {
        return members;
    }

    public void setMembers(Set<WalletMember> members) {
        this.members = members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(getId(), wallet.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getId());
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + getId() +
                ", walletName='" + walletName + '\'' +
                ", walletType=" + walletType +
                ", balance=" + balance +
                ", currency='" + currency + '\'' +
                ", isFamilyWallet=" + isFamilyWallet +
                ", isActive=" + isActive +
                '}';
    }
}
