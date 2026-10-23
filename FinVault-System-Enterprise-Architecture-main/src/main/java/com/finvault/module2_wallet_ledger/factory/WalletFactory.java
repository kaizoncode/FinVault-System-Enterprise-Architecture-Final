package com.finvault.module2_wallet_ledger.factory;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module2_wallet_ledger.dto.WalletRequest;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletMember;
import com.finvault.module2_wallet_ledger.entity.WalletMemberRole;
import com.finvault.module2_wallet_ledger.entity.WalletType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * WalletFactory: Áp dụng Design Pattern Factory để khởi tạo các loại Ví chuyên biệt
 *
 * Chiến lược khởi tạo theo từng loại ví:
 * - CASH: Khởi tạo số dư ban đầu, không yêu cầu thông tin ngân hàng
 * - BANK: Bắt buộc số tài khoản và tên ngân hàng liên kết
 * - CREDIT_CARD: Khởi tạo hạn mức tín dụng (monthlySpendingLimit)
 * - FAMILY_FUND: Thiết lập isFamilyWallet = true, tự động gán chủ sở hữu làm ADMIN của ví
 * ==============================================================================
 */
@Component
public class WalletFactory {

    /**
     * Tạo thực thể Ví phù hợp dựa trên WalletRequest và User sở hữu
     */
    public Wallet createWallet(User owner, WalletRequest request) {
        if (owner == null) {
            throw new BusinessException("INVALID_OWNER", "Chủ sở hữu ví không hợp lệ");
        }
        if (request.getWalletType() == null) {
            throw new BusinessException("INVALID_WALLET_TYPE", "Loại ví không được để trống");
        }

        BigDecimal initialBalance = (request.getInitialBalance() != null)
                ? request.getInitialBalance()
                : BigDecimal.ZERO;

        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số dư ban đầu không được âm");
        }
        if (request.getMonthlySpendingLimit() != null && request.getMonthlySpendingLimit().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_AMOUNT", "Hạn mức chi tiêu không được âm");
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().trim().toUpperCase()
                : "VND";

        Wallet wallet = new Wallet();
        wallet.setUser(owner);
        wallet.setWalletName(request.getWalletName().trim());
        wallet.setWalletType(request.getWalletType());
        wallet.setInitialBalance(initialBalance);
        wallet.setBalance(initialBalance);
        wallet.setCurrency(currency);
        wallet.setIsActive(true);

        switch (request.getWalletType()) {
            case CASH -> configureCashWallet(wallet);
            case BANK -> configureBankWallet(wallet, request);
            case CREDIT_CARD -> configureCreditCardWallet(wallet, request);
            case FAMILY_FUND -> configureFamilyFundWallet(wallet, owner, request);
            case SAVINGS -> configureSavingsWallet(wallet, request);
            default -> throw new BusinessException("UNSUPPORTED_WALLET_TYPE", "Loại ví không được hỗ trợ: " + request.getWalletType());
        }

        return wallet;
    }

    private void configureSavingsWallet(Wallet wallet, WalletRequest request) {
        wallet.setIsFamilyWallet(false);
        wallet.setBankAccountNumber(request.getBankAccountNumber() != null ? request.getBankAccountNumber().trim() : null);
        wallet.setBankName(request.getBankName() != null ? request.getBankName().trim() : null);
        wallet.setMonthlySpendingLimit(BigDecimal.ZERO);
    }

    private void configureCashWallet(Wallet wallet) {
        wallet.setIsFamilyWallet(false);
        wallet.setBankAccountNumber(null);
        wallet.setBankName(null);
        wallet.setMonthlySpendingLimit(BigDecimal.ZERO);
    }

    private void configureBankWallet(Wallet wallet, WalletRequest request) {
        if (request.getBankAccountNumber() == null || request.getBankAccountNumber().isBlank()) {
            throw new BusinessException("BANK_ACCOUNT_REQUIRED", "Ví ngân hàng yêu cầu cung cấp số tài khoản");
        }
        if (request.getBankName() == null || request.getBankName().isBlank()) {
            throw new BusinessException("BANK_NAME_REQUIRED", "Ví ngân hàng yêu cầu cung cấp tên ngân hàng");
        }
        wallet.setIsFamilyWallet(false);
        wallet.setBankAccountNumber(request.getBankAccountNumber().trim());
        wallet.setBankName(request.getBankName().trim());
        wallet.setMonthlySpendingLimit(BigDecimal.ZERO);
    }

    private void configureCreditCardWallet(Wallet wallet, WalletRequest request) {
        wallet.setIsFamilyWallet(false);
        wallet.setBankAccountNumber(request.getBankAccountNumber() != null ? request.getBankAccountNumber().trim() : null);
        wallet.setBankName(request.getBankName() != null ? request.getBankName().trim() : "Thẻ tín dụng");
        BigDecimal limit = (request.getMonthlySpendingLimit() != null && request.getMonthlySpendingLimit().compareTo(BigDecimal.ZERO) > 0)
                ? request.getMonthlySpendingLimit()
                : new BigDecimal("10000000"); // Hạn mức mặc định 10 triệu
        wallet.setMonthlySpendingLimit(limit);
    }

    private void configureFamilyFundWallet(Wallet wallet, User owner, WalletRequest request) {
        wallet.setIsFamilyWallet(true);
        wallet.setBankAccountNumber(request.getBankAccountNumber() != null ? request.getBankAccountNumber().trim() : null);
        wallet.setBankName(request.getBankName() != null ? request.getBankName().trim() : null);
        BigDecimal limit = (request.getMonthlySpendingLimit() != null)
                ? request.getMonthlySpendingLimit()
                : new BigDecimal("50000000"); // Hạn mức quỹ gia đình mặc định 50 triệu
        wallet.setMonthlySpendingLimit(limit);

        // Tự động thêm chủ quỹ gia đình với vai trò ADMIN
        WalletMember ownerMember = new WalletMember(wallet, owner, WalletMemberRole.ADMIN, limit);
        wallet.addMember(ownerMember);
    }
}
