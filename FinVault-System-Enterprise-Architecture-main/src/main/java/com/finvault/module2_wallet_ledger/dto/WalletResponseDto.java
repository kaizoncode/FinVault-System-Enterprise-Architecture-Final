package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletResponseDto: DTO trả về thông tin chi tiết Ví tài chính
 * ==============================================================================
 */
public class WalletResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long ownerId;
    private String ownerUsername;
    private String walletName;
    private WalletType walletType;
    private BigDecimal balance;
    private String currency;
    private String bankAccountNumber;
    private String bankName;
    private Boolean isFamilyWallet;
    private BigDecimal monthlySpendingLimit;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private List<WalletMemberDto> members;

    public WalletResponseDto() {
    }

    public static WalletResponseDto fromEntity(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        WalletResponseDto dto = new WalletResponseDto();
        dto.setId(wallet.getId());
        if (wallet.getUser() != null) {
            dto.setOwnerId(wallet.getUser().getId());
            dto.setOwnerUsername(wallet.getUser().getUsername());
        }
        dto.setWalletName(wallet.getWalletName());
        dto.setWalletType(wallet.getWalletType());
        dto.setBalance(wallet.getBalance());
        dto.setCurrency(wallet.getCurrency());
        dto.setBankAccountNumber(wallet.getBankAccountNumber());
        dto.setBankName(wallet.getBankName());
        dto.setIsFamilyWallet(wallet.getIsFamilyWallet());
        dto.setMonthlySpendingLimit(wallet.getMonthlySpendingLimit());
        dto.setIsActive(wallet.getIsActive());
        dto.setCreatedAt(wallet.getCreatedAt());

        if (wallet.getMembers() != null) {
            dto.setMembers(wallet.getMembers().stream()
                    .map(WalletMemberDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
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
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
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

    public String getAccountNumber() {
        return bankAccountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.bankAccountNumber = accountNumber;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<WalletMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<WalletMemberDto> members) {
        this.members = members;
    }
}
