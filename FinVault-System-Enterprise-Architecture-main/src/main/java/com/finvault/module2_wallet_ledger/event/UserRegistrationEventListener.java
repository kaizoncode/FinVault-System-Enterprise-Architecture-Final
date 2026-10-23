package com.finvault.module2_wallet_ledger.event;

import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.event.UserRegisteredEvent;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletMember;
import com.finvault.module2_wallet_ledger.entity.WalletMemberRole;
import com.finvault.module2_wallet_ledger.entity.WalletType;
import com.finvault.module2_wallet_ledger.repository.CategoryRepository;
import com.finvault.module2_wallet_ledger.repository.WalletMemberRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.BudgetType;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * UserRegistrationEventListener: Tự động khởi tạo hệ sinh thái tài chính cho người dùng mới
 *
 * Tính năng:
 * 1. Khởi tạo ngay "Ví Chi Tiêu Cá Nhân" với số dư thử nghiệm 10.000.000 VND
 * 2. Tự động liên kết tài khoản mới vào Quỹ chung gia đình với vai trò MEMBER (Hạn mức 10.000.000 VND)
 *    để kiểm thử ngay luồng duyệt chi với Gia trưởng (ROLE_FAMILY_HEAD)
 * 3. Tự động thiết lập ngân sách thông minh 50/30/20 cho người dùng
 * ==============================================================================
 */
@Component
public class UserRegistrationEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserRegistrationEventListener.class);

    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    public UserRegistrationEventListener(
            WalletRepository walletRepository,
            WalletMemberRepository walletMemberRepository,
            CategoryRepository categoryRepository,
            BudgetRepository budgetRepository) {
        this.walletRepository = walletRepository;
        this.walletMemberRepository = walletMemberRepository;
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
    }

    @EventListener
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        User user = event.getUser();
        if (user == null) return;

        log.info("Bắt đầu tự động thiết lập hệ sinh thái tài chính ban đầu cho user: [{}]", user.getUsername());

        // 1. Tạo ví tiền mặt cá nhân đầu tiên
        Wallet personalWallet = new Wallet();
        personalWallet.setUser(user);
        personalWallet.setWalletName("Ví Chi Tiêu Cá Nhân");
        personalWallet.setWalletType(WalletType.CASH);
        personalWallet.setBalance(new BigDecimal("10000000.00")); // 10.000.000 VND số dư ban đầu
        personalWallet.setInitialBalance(new BigDecimal("10000000.00"));
        personalWallet.setCurrency("VND");
        personalWallet.setIsFamilyWallet(false);
        personalWallet.setIsActive(true);
        personalWallet.setCreatedBy(user.getUsername());
        personalWallet.setUpdatedBy(user.getUsername());
        Wallet savedWallet = walletRepository.save(personalWallet);
        log.info("Đã tạo Ví cá nhân ID [{}] cho [{}]", savedWallet.getId(), user.getUsername());

        // 2. Việc tham gia Quỹ gia đình được quản lý thông qua lời mời (Invitation) và phê duyệt rõ ràng từ Chủ hộ (Owner)
        // Không tự động gán người dùng lạ vào quỹ gia đình để đảm bảo an toàn bảo mật và cô lập dữ liệu người dùng.

        // 3. Khởi tạo 2 ngân sách thông minh 50/30/20 thử nghiệm
        try {
            LocalDate today = LocalDate.now();
            LocalDate nextMonth = today.plusMonths(1);

            // Ngân sách Thiết yếu (Needs 50%)
            Budget needsBudget = new Budget();
            needsBudget.setUser(user);
            needsBudget.setWallet(savedWallet);
            needsBudget.setBudgetName("Chi Tiêu Thiết Yếu (Needs 50%)");
            needsBudget.setBudgetType(BudgetType.RULE_50_30_20);
            needsBudget.setAllocationType(AllocationType.NEEDS);
            needsBudget.setAllocatedAmount(new BigDecimal("8000000.00"));
            needsBudget.setSpentAmount(BigDecimal.ZERO);
            needsBudget.setStartDate(today);
            needsBudget.setEndDate(nextMonth);
            needsBudget.setIsActive(true);
            needsBudget.setCreatedBy(user.getUsername());
            needsBudget.setUpdatedBy(user.getUsername());
            categoryRepository.findByIdAndIsDeletedFalse(5L).ifPresent(needsBudget::setCategory);
            budgetRepository.save(needsBudget);

            // Ngân sách Sở thích (Wants 30%)
            Budget wantsBudget = new Budget();
            wantsBudget.setUser(user);
            wantsBudget.setWallet(savedWallet);
            wantsBudget.setBudgetName("Sở Thích & Mua Sắm (Wants 30%)");
            wantsBudget.setBudgetType(BudgetType.RULE_50_30_20);
            wantsBudget.setAllocationType(AllocationType.WANTS);
            wantsBudget.setAllocatedAmount(new BigDecimal("5000000.00"));
            wantsBudget.setSpentAmount(BigDecimal.ZERO);
            wantsBudget.setStartDate(today);
            wantsBudget.setEndDate(nextMonth);
            wantsBudget.setIsActive(true);
            wantsBudget.setCreatedBy(user.getUsername());
            wantsBudget.setUpdatedBy(user.getUsername());
            categoryRepository.findByIdAndIsDeletedFalse(8L).ifPresent(wantsBudget::setCategory);
            budgetRepository.save(wantsBudget);

            log.info("Đã khởi tạo bộ ngân sách 50/30/20 ban đầu cho user [{}]", user.getUsername());
        } catch (Exception ex) {
            log.warn("Không thể tạo ngân sách tự động cho [{}]: {}", user.getUsername(), ex.getMessage());
        }
    }
}
