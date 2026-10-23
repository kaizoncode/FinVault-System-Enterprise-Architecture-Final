package com.finvault.module3_budget_goals.event;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.event.TransactionCreatedEvent;
import com.finvault.module2_wallet_ledger.event.TransactionDeletedEvent;
import com.finvault.module2_wallet_ledger.repository.CategoryRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.dto.BudgetAlertNotificationDto;
import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.BudgetType;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import com.finvault.module3_budget_goals.rule.BudgetAlertRuleEngine;
import com.finvault.module3_budget_goals.websocket.BudgetAlertWebSocketSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * TransactionBudgetEventListener: Thành phần lắng nghe sự kiện giao dịch tài chính (Observer Pattern)
 *
 * Quy trình xử lý tự động:
 * 1. Lắng nghe TransactionCreatedEvent từ Module 2 (Core Ledger).
 * 2. Lọc các giao dịch chi tiêu (EXPENSE) đã được phê duyệt (APPROVED).
 * 3. Trích xuất primitive IDs từ Event nhằm chống LazyInitializationException trong ngữ cảnh @Async.
 * 4. Tìm các ngân sách theo Danh mục cụ thể VÀ ngân sách phân bổ chung 50/30/20 (category IS NULL).
 * 5. Tự động ghi nhận số tiền chi tiêu (recordExpense) và cập nhật số dư ngân sách.
 * 6. Kích hoạt BudgetAlertRuleEngine (Strategy Pattern) để kiểm tra ngưỡng 80% / 100%.
 * 7. Tự động đẩy cảnh báo tức thì qua WebSocket tới màn hình chính của người dùng.
 * ==============================================================================
 */
@Component
public class TransactionBudgetEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionBudgetEventListener.class);

    private final BudgetRepository budgetRepository;
    private final BudgetAlertRuleEngine alertRuleEngine;
    private final BudgetAlertWebSocketSender webSocketSender;
    private final CategoryRepository categoryRepository;
    private final WalletRepository walletRepository;

    public TransactionBudgetEventListener(
            BudgetRepository budgetRepository,
            BudgetAlertRuleEngine alertRuleEngine,
            BudgetAlertWebSocketSender webSocketSender) {
        this(budgetRepository, alertRuleEngine, webSocketSender, null, null);
    }

    @Autowired
    public TransactionBudgetEventListener(
            BudgetRepository budgetRepository,
            BudgetAlertRuleEngine alertRuleEngine,
            BudgetAlertWebSocketSender webSocketSender,
            CategoryRepository categoryRepository,
            WalletRepository walletRepository) {
        this.budgetRepository = budgetRepository;
        this.alertRuleEngine = alertRuleEngine;
        this.webSocketSender = webSocketSender;
        this.categoryRepository = categoryRepository;
        this.walletRepository = walletRepository;
    }

    @Async
    @EventListener
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void onTransactionCreated(TransactionCreatedEvent event) {
        if (event == null) {
            return;
        }

        // Chỉ xử lý các giao dịch chi tiêu đã được phê duyệt chính thức
        if (event.getTransactionType() != TransactionType.EXPENSE || event.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return;
        }

        Long userId = event.getUserId();
        String username = event.getUsername();
        if (userId == null) {
            log.warn("Bỏ qua cập nhật ngân sách vì userId trong sự kiện giao dịch là null (txId: {})", event.getTransactionId());
            return;
        }

        BigDecimal amount = event.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Bỏ qua sự kiện chi tiêu với số tiền không hợp lệ hoặc <= 0");
            return;
        }

        LocalDate txDate = (event.getTransactionDate() != null)
                ? event.getTransactionDate().toLocalDate()
                : LocalDate.now();
        Long categoryId = event.getCategoryId();
        Long walletId = event.getWalletId();

        log.debug("Observer Pattern: Nhận sự kiện chi tiêu ID [{}] số tiền {} VND của user [{}]",
                event.getTransactionId(), amount, username);

        List<Budget> matchingBudgets = findMatchingBudgets(userId, walletId, categoryId, txDate, event.getTransaction());

        if (matchingBudgets.isEmpty()) {
            log.debug("Không tìm thấy ngân sách nào đang áp dụng cho giao dịch ID [{}] của user [{}]", event.getTransactionId(), userId);
            return;
        }

        for (Budget budget : matchingBudgets) {
            // Cập nhật số tiền đã chi
            budget.recordExpense(amount);

            // Đánh giá quy tắc cảnh báo bằng Rule Engine (Strategy Pattern)
            Optional<BudgetAlertNotificationDto> alertOpt = alertRuleEngine.evaluateAndCreateAlert(budget);

            // Lưu ngân sách với số tiền chi tiêu mới và cờ cảnh báo đã cập nhật
            budgetRepository.save(budget);

            // Nếu chạm ngưỡng cảnh báo -> Bắn WebSocket tới người dùng
            alertOpt.ifPresent(alert -> {
                String targetUser = (budget.getUser() != null && budget.getUser().getUsername() != null)
                        ? budget.getUser().getUsername()
                        : username;
                if (targetUser != null) {
                    webSocketSender.sendAlert(targetUser, alert);
                    log.warn("Đã phát hiện và phát cảnh báo ngân sách [{}] cho người dùng [{}] qua WebSocket",
                            budget.getBudgetName(), targetUser);
                }
            });
        }
    }

    /**
     * Lắng nghe sự kiện xóa giao dịch để tự động hoàn tác (giảm) spentAmount của ngân sách tương ứng.
     */
    @Async
    @EventListener
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public void onTransactionDeleted(TransactionDeletedEvent event) {
        if (event == null) {
            return;
        }

        // Chỉ hoàn tác chi tiêu đối với các giao dịch chi tiêu (EXPENSE) đã được phê duyệt (APPROVED)
        if (event.getTransactionType() != TransactionType.EXPENSE || event.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return;
        }

        Long userId = event.getUserId();
        String username = event.getUsername();
        if (userId == null) {
            log.warn("Bỏ qua giảm ngân sách vì userId trong sự kiện xóa giao dịch là null (txId: {})", event.getTransactionId());
            return;
        }

        BigDecimal amount = event.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("Bỏ qua sự kiện xóa chi tiêu với số tiền không hợp lệ hoặc <= 0");
            return;
        }

        LocalDate txDate = (event.getTransactionDate() != null)
                ? event.getTransactionDate().toLocalDate()
                : LocalDate.now();
        Long categoryId = event.getCategoryId();
        Long walletId = event.getWalletId();

        log.debug("Observer Pattern: Nhận sự kiện XÓA chi tiêu ID [{}] số tiền {} VND của user [{}]",
                event.getTransactionId(), amount, username);

        List<Budget> matchingBudgets = findMatchingBudgets(userId, walletId, categoryId, txDate, event.getTransaction());

        if (matchingBudgets.isEmpty()) {
            log.debug("Không tìm thấy ngân sách nào đang áp dụng để giảm cho giao dịch xóa ID [{}] của user [{}]",
                    event.getTransactionId(), userId);
            return;
        }

        for (Budget budget : matchingBudgets) {
            // Giảm số tiền đã chi
            budget.revertExpense(amount);

            // Reset lại cờ cảnh báo nếu sau khi hoàn tác chi tiêu giảm xuống dưới ngưỡng 100% hoặc 80%
            if (budget.getAllocatedAmount() != null && budget.getSpentAmount() != null) {
                BigDecimal eightyPct = budget.getAllocatedAmount().multiply(new BigDecimal("0.80"));
                if (budget.getSpentAmount().compareTo(budget.getAllocatedAmount()) < 0) {
                    budget.setAlertThreshold100Sent(false);
                }
                if (budget.getSpentAmount().compareTo(eightyPct) < 0) {
                    budget.setAlertThreshold80Sent(false);
                }
            }

            budgetRepository.save(budget);
            log.info("Đã giảm spentAmount cho ngân sách [{}] (mới: {} VND) sau khi xóa giao dịch ID [{}] của user [{}]",
                    budget.getBudgetName(), budget.getSpentAmount(), event.getTransactionId(), username);
        }
    }

    private List<Budget> findMatchingBudgets(Long userId, Long walletId, Long categoryId, LocalDate txDate, Transaction tx) {
        AllocationType targetAllocation = resolveAllocationType(categoryId, tx);

        Set<Long> processedBudgetIds = new HashSet<>();
        List<Budget> matchingBudgets = new ArrayList<>();

        collectUserMatchingBudgets(userId, walletId, categoryId, txDate, targetAllocation, false, processedBudgetIds, matchingBudgets);

        // Nếu giao dịch phát sinh trên Quỹ Gia Đình thuộc chủ sở hữu khác, kiểm tra cả ngân sách gắn với ví gia đình đó
        Wallet txWallet = resolveWallet(walletId, tx);
        if (txWallet != null && Boolean.TRUE.equals(txWallet.getIsFamilyWallet())
                && txWallet.getUser() != null && !txWallet.getUser().getId().equals(userId)) {
            collectUserMatchingBudgets(txWallet.getUser().getId(), walletId, categoryId, txDate, targetAllocation, true, processedBudgetIds, matchingBudgets);
        }

        return matchingBudgets;
    }

    private void collectUserMatchingBudgets(Long targetUserId, Long walletId, Long categoryId, LocalDate txDate,
                                            AllocationType targetAllocation, boolean requireWalletMatch,
                                            Set<Long> processedBudgetIds, List<Budget> matchingBudgets) {
        List<Budget> categoryBudgets = (categoryId != null)
                ? budgetRepository.findActiveBudgetsByCategoryAndDate(targetUserId, categoryId, txDate)
                : Collections.emptyList();

        for (Budget b : categoryBudgets) {
            if (b == null || b.getId() == null) continue;
            if (requireWalletMatch && (b.getWallet() == null || !b.getWallet().getId().equals(walletId))) continue;
            if (b.getWallet() != null && (walletId == null || !b.getWallet().getId().equals(walletId))) continue;
            if (processedBudgetIds.add(b.getId())) {
                matchingBudgets.add(b);
            }
        }

        List<Budget> generalBudgets = budgetRepository.findActiveBudgetsWithNullCategoryByUserAndDate(targetUserId, txDate);
        boolean hasMultipleAllocationBuckets = generalBudgets.stream()
                .map(Budget::getAllocationType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count() > 1;

        for (Budget gb : generalBudgets) {
            if (gb == null || gb.getId() == null) continue;
            if (requireWalletMatch && (gb.getWallet() == null || !gb.getWallet().getId().equals(walletId))) continue;
            if (gb.getWallet() != null && (walletId == null || !gb.getWallet().getId().equals(walletId))) continue;
            if (gb.getBudgetType() == BudgetType.RULE_50_30_20 || hasMultipleAllocationBuckets) {
                if (gb.getAllocationType() != null && gb.getAllocationType() != targetAllocation) {
                    continue;
                }
            }
            if (processedBudgetIds.add(gb.getId())) {
                matchingBudgets.add(gb);
            }
        }
    }

    private AllocationType resolveAllocationType(Long categoryId, Transaction tx) {
        if (Long.valueOf(8L).equals(categoryId)) {
            return AllocationType.WANTS;
        }
        String categoryName = null;
        if (categoryId != null && categoryRepository != null) {
            try {
                categoryName = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                        .map(Category::getCategoryName)
                        .orElse(null);
            } catch (Exception ignored) {
            }
        }
        if (categoryName == null && tx != null) {
            try {
                if (tx.getCategory() != null) {
                    categoryName = tx.getCategory().getCategoryName();
                }
            } catch (Exception ignored) {
            }
        }
        if (categoryName != null) {
            String lower = categoryName.toLowerCase();
            if (lower.contains("mua sắm") || lower.contains("mua sam")
                    || lower.contains("giải trí") || lower.contains("giai tri")
                    || lower.contains("sở thích") || lower.contains("so thich")
                    || lower.contains("du lịch") || lower.contains("du lich")
                    || lower.contains("wants")) {
                return AllocationType.WANTS;
            }
            if (lower.contains("tiết kiệm") || lower.contains("tiet kiem")
                    || lower.contains("đầu tư") || lower.contains("dau tu")
                    || lower.contains("tích lũy") || lower.contains("tich luy")
                    || lower.contains("savings")) {
                return AllocationType.SAVINGS;
            }
        }
        return AllocationType.NEEDS;
    }

    private Wallet resolveWallet(Long walletId, Transaction tx) {
        if (walletId != null && walletRepository != null) {
            try {
                return walletRepository.findByIdAndIsDeletedFalse(walletId).orElse(null);
            } catch (Exception ignored) {
            }
        }
        if (tx != null) {
            try {
                return tx.getWallet();
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
