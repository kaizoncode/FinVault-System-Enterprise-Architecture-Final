package com.finvault.module2_wallet_ledger.entity;

import com.finvault.module1_security_rbac.entity.User;
import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletMember Entity: Thực thể liên kết người dùng vào Quỹ tài chính gia đình
 *
 * Tính năng chính:
 * - Phân quyền vai trò: ADMIN, MEMBER, VIEWER
 * - Thiết lập hạn mức chi tiêu cá nhân (spending_limit) độc lập cho từng thành viên
 * - Đảm bảo ràng buộc UNIQUE (wallet_id, user_id)
 * ==============================================================================
 */
@Entity
@Table(name = "wallet_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallet_user", columnNames = {"wallet_id", "user_id"})
})
public class WalletMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 30)
    private WalletMemberRole memberRole = WalletMemberRole.MEMBER;

    @Column(name = "spending_limit", precision = 18, scale = 2)
    private BigDecimal spendingLimit = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public WalletMember() {
    }

    public WalletMember(Wallet wallet, User user, WalletMemberRole memberRole, BigDecimal spendingLimit) {
        this.wallet = wallet;
        this.user = user;
        this.memberRole = (memberRole != null) ? memberRole : WalletMemberRole.MEMBER;
        this.spendingLimit = (spendingLimit != null) ? spendingLimit : BigDecimal.ZERO;
        this.isActive = true;
        this.joinedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WalletMemberRole getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(WalletMemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public BigDecimal getSpendingLimit() {
        return spendingLimit;
    }

    public void setSpendingLimit(BigDecimal spendingLimit) {
        this.spendingLimit = spendingLimit;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletMember that = (WalletMember) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "WalletMember{" +
                "id=" + id +
                ", memberRole=" + memberRole +
                ", spendingLimit=" + spendingLimit +
                ", isActive=" + isActive +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
