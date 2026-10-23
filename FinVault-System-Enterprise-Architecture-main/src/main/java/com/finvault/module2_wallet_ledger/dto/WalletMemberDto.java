package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.WalletMember;
import com.finvault.module2_wallet_ledger.entity.WalletMemberRole;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletMemberDto: DTO quản lý thành viên Quỹ tài chính gia đình
 * ==============================================================================
 */
public class WalletMemberDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long walletId;

    private Long userId;

    private String username;
    private String fullName;

    @NotNull(message = "Vai trò thành viên không được để trống")
    private WalletMemberRole memberRole = WalletMemberRole.MEMBER;

    private BigDecimal spendingLimit = BigDecimal.ZERO;

    private Boolean isActive;
    private LocalDateTime joinedAt;

    public WalletMemberDto() {
    }

    public static WalletMemberDto fromEntity(WalletMember member) {
        if (member == null) {
            return null;
        }
        WalletMemberDto dto = new WalletMemberDto();
        dto.setId(member.getId());
        dto.setWalletId(member.getWallet() != null ? member.getWallet().getId() : null);
        if (member.getUser() != null) {
            dto.setUserId(member.getUser().getId());
            dto.setUsername(member.getUser().getUsername());
            dto.setFullName(member.getUser().getFullName());
        }
        dto.setMemberRole(member.getMemberRole());
        dto.setSpendingLimit(member.getSpendingLimit());
        dto.setIsActive(member.getIsActive());
        dto.setJoinedAt(member.getJoinedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
}
