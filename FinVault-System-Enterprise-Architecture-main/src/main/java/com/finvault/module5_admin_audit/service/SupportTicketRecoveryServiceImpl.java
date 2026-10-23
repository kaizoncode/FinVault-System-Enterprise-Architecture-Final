package com.finvault.module5_admin_audit.service;

import com.finvault.common.BusinessException;
import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.event.TransactionCreatedEvent;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.SavingGoal;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import com.finvault.module3_budget_goals.repository.SavingGoalRepository;
import com.finvault.module5_admin_audit.aop.Auditable;
import com.finvault.module5_admin_audit.dto.DataRecoveryResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SupportTicketRecoveryServiceImpl: Triển khai khôi phục các thực thể bị xóa nhầm (Soft Delete Recovery)
 *
 * Tính năng chính:
 * - Quét các ví, giao dịch, ngân sách, mục tiêu đã bị đánh dấu isDeleted = true
 * - Khôi phục trạng thái hoạt động bình thường
 * - Với giao dịch đã được phê duyệt, tự động hoàn trả số dư vào ví một cách an toàn qua Pessimistic Lock
 * - Tự động ghi lại nhật ký kiểm toán hành động khôi phục dữ liệu
 * ==============================================================================
 */
@Service
@Transactional
public class SupportTicketRecoveryServiceImpl implements SupportTicketRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(SupportTicketRecoveryServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingGoalRepository savingGoalRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SupportTicketRecoveryServiceImpl(WalletRepository walletRepository,
                                            TransactionRepository transactionRepository,
                                            BudgetRepository budgetRepository,
                                            SavingGoalRepository savingGoalRepository) {
        this(walletRepository, transactionRepository, budgetRepository, savingGoalRepository, null);
    }

    @Autowired
    public SupportTicketRecoveryServiceImpl(WalletRepository walletRepository,
                                            TransactionRepository transactionRepository,
                                            BudgetRepository budgetRepository,
                                            SavingGoalRepository savingGoalRepository,
                                            ApplicationEventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.savingGoalRepository = savingGoalRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataRecoveryResponseDto> getSoftDeletedItems() {
        List<DataRecoveryResponseDto> result = new ArrayList<>();

        // 1. Quét các Ví bị xóa mềm
        List<Wallet> deletedWallets = entityManager.createQuery(
                "SELECT w FROM Wallet w WHERE w.isDeleted = true ORDER BY w.updatedAt DESC", Wallet.class)
                .getResultList();
        for (Wallet w : deletedWallets) {
            result.add(new DataRecoveryResponseDto(
                    "WALLET",
                    w.getId(),
                    w.getWalletName(),
                    (w.getUser() != null) ? w.getUser().getUsername() : "N/A",
                    w.getUpdatedAt(),
                    "Số dư: " + w.getBalance() + " " + w.getCurrency()
            ));
        }

        // 2. Quét các Giao dịch bị xóa mềm
        List<Transaction> deletedTransactions = entityManager.createQuery(
                "SELECT t FROM Transaction t WHERE t.isDeleted = true ORDER BY t.transactionDate DESC", Transaction.class)
                .getResultList();
        Set<Long> deletedTxIds = new HashSet<>();
        for (Transaction t : deletedTransactions) {
            if (t.getId() != null) {
                deletedTxIds.add(t.getId());
            }
        }
        for (Transaction t : deletedTransactions) {
            if (t.getTransactionType() == TransactionType.TRANSFER
                    && t.getTransferDirection() == TransferDirection.INCOMING
                    && t.getLinkedTransactionId() != null
                    && deletedTxIds.contains(t.getLinkedTransactionId())) {
                continue;
            }
            result.add(new DataRecoveryResponseDto(
                    "TRANSACTION",
                    t.getId(),
                    "TX-" + t.getId() + " (" + t.getDescription() + ")",
                    (t.getUser() != null) ? t.getUser().getUsername() : "N/A",
                    (t.getUpdatedAt() != null) ? t.getUpdatedAt() : t.getTransactionDate(),
                    "Số tiền: " + t.getAmount() + " VND, Loại: " + t.getTransactionType()
            ));
        }

        // 3. Quét các Ngân sách bị xóa mềm
        List<Budget> deletedBudgets = entityManager.createQuery(
                "SELECT b FROM Budget b WHERE b.isDeleted = true ORDER BY b.updatedAt DESC", Budget.class)
                .getResultList();
        for (Budget b : deletedBudgets) {
            result.add(new DataRecoveryResponseDto(
                    "BUDGET",
                    b.getId(),
                    b.getBudgetName(),
                    (b.getUser() != null) ? b.getUser().getUsername() : "N/A",
                    b.getUpdatedAt(),
                    "Hạn mức: " + b.getAllocatedAmount() + " VND"
            ));
        }

        // 4. Quét các Mục tiêu tiết kiệm bị xóa mềm
        List<SavingGoal> deletedGoals = entityManager.createQuery(
                "SELECT g FROM SavingGoal g WHERE g.isDeleted = true ORDER BY g.updatedAt DESC", SavingGoal.class)
                .getResultList();
        for (SavingGoal g : deletedGoals) {
            result.add(new DataRecoveryResponseDto(
                    "SAVING_GOAL",
                    g.getId(),
                    g.getGoalName(),
                    (g.getUser() != null) ? g.getUser().getUsername() : "N/A",
                    g.getUpdatedAt(),
                    "Mục tiêu: " + g.getTargetAmount() + " VND"
            ));
        }

        return result;
    }

    @Override
    @Auditable(action = "RESTORE_WALLET", module = "SYSTEM_RECOVERY", entityName = "wallets")
    public void restoreWallet(Long walletId, String adminUsername) {
        log.info("Admin [{}] khôi phục ví ID: {}", adminUsername, walletId);

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví ID: " + walletId));

        if (!Boolean.TRUE.equals(wallet.getIsDeleted())) {
            throw new BusinessException("NOT_DELETED", "Bản ghi không ở trạng thái bị xóa");
        }

        wallet.setIsDeleted(false);
        wallet.setIsActive(true);
        walletRepository.save(wallet);
        log.info("Khôi phục thành công ví ID: {}", walletId);
    }

    @Override
    @Auditable(action = "RESTORE_TRANSACTION", module = "SYSTEM_RECOVERY", entityName = "transactions")
    public void restoreTransaction(Long transactionId, String adminUsername) {
        log.info("Admin [{}] khôi phục giao dịch ID: {}", adminUsername, transactionId);

        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException("TRANSACTION_NOT_FOUND", "Không tìm thấy giao dịch ID: " + transactionId));

        if (!Boolean.TRUE.equals(tx.getIsDeleted())) {
            throw new BusinessException("TRANSACTION_NOT_DELETED", "Giao dịch không ở trạng thái bị xóa");
        }

        // Nếu giao dịch đã được phê duyệt, đồng bộ lại số dư ví tương ứng
        if (tx.getApprovalStatus() == ApprovalStatus.APPROVED && tx.getWallet() != null) {
            if (tx.getTransactionType() == TransactionType.INCOME) {
                Wallet wallet = walletRepository.findByIdForUpdate(tx.getWallet().getId())
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví liên kết ID: " + tx.getWallet().getId()));
                if (Boolean.TRUE.equals(wallet.getIsDeleted())) {
                    throw new BusinessException("WALLET_DELETED", "Ví liên kết (ID: " + wallet.getId() + ") đã bị xóa. Vui lòng khôi phục ví trước khi khôi phục giao dịch");
                }
                wallet.deposit(tx.getAmount());
                walletRepository.save(wallet);
            } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                Wallet wallet = walletRepository.findByIdForUpdate(tx.getWallet().getId())
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví liên kết ID: " + tx.getWallet().getId()));
                if (Boolean.TRUE.equals(wallet.getIsDeleted())) {
                    throw new BusinessException("WALLET_DELETED", "Ví liên kết (ID: " + wallet.getId() + ") đã bị xóa. Vui lòng khôi phục ví trước khi khôi phục giao dịch");
                }
                wallet.withdraw(tx.getAmount());
                walletRepository.save(wallet);
            } else if (tx.getTransactionType() == TransactionType.TRANSFER) {
                if (tx.getTargetWallet() == null) {
                    throw new BusinessException("TARGET_WALLET_NOT_FOUND", "Không tìm thấy ví liên kết đối ứng của giao dịch chuyển tiền");
                }

                Long idA = tx.getWallet().getId();
                Long idB = tx.getTargetWallet().getId();
                if (idA.equals(idB)) {
                    throw new BusinessException("INVALID_TRANSFER", "Ví nguồn và ví đích không thể trùng nhau");
                }
                Long firstId = Math.min(idA, idB);
                Long secondId = Math.max(idA, idB);

                Wallet firstWallet = walletRepository.findByIdForUpdate(firstId)
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví ID: " + firstId));
                Wallet secondWallet = walletRepository.findByIdForUpdate(secondId)
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví ID: " + secondId));

                Wallet lockedWallet = tx.getWallet().getId().equals(firstId) ? firstWallet : secondWallet;
                Wallet lockedTargetWallet = tx.getTargetWallet().getId().equals(firstId) ? firstWallet : secondWallet;

                if (Boolean.TRUE.equals(lockedTargetWallet.getIsDeleted())) {
                    throw new BusinessException("TARGET_WALLET_DELETED", "Ví đích (ID: " + lockedTargetWallet.getId() + ") đã bị xóa. Vui lòng khôi phục ví đích trước khi khôi phục giao dịch chuyển tiền");
                }
                if (Boolean.TRUE.equals(lockedWallet.getIsDeleted())) {
                    throw new BusinessException("WALLET_DELETED", "Ví chính (ID: " + lockedWallet.getId() + ") đã bị xóa. Vui lòng khôi phục ví trước khi khôi phục giao dịch");
                }

                // Xác định chiều chuyển khoản (INCOMING / OUTGOING)
                boolean isIncoming = (tx.getTransferDirection() != null && tx.getTransferDirection() == TransferDirection.INCOMING)
                        || (tx.getNote() != null && tx.getNote().trim().toLowerCase().startsWith("nhận từ"));

                Wallet sourceWallet = isIncoming ? lockedTargetWallet : lockedWallet;
                Wallet destWallet = isIncoming ? lockedWallet : lockedTargetWallet;

                sourceWallet.withdraw(tx.getAmount());
                destWallet.deposit(tx.getAmount());

                walletRepository.save(sourceWallet);
                walletRepository.save(destWallet);

                // Khôi phục luôn bản ghi đối ứng liên kết nếu có
                if (tx.getLinkedTransactionId() != null) {
                    transactionRepository.findById(tx.getLinkedTransactionId()).ifPresent(linkedTx -> {
                        if (Boolean.TRUE.equals(linkedTx.getIsDeleted())) {
                            linkedTx.setIsDeleted(false);
                            transactionRepository.save(linkedTx);
                            log.info("Đã khôi phục bản ghi đối ứng ID: [{}] của giao dịch chuyển khoản [{}]", linkedTx.getId(), tx.getId());
                        }
                    });
                }
            }
        }

        tx.setIsDeleted(false);
        Transaction savedTx = transactionRepository.save(tx);
        if (savedTx.getApprovalStatus() == ApprovalStatus.APPROVED && eventPublisher != null) {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
        }
        log.info("Khôi phục thành công giao dịch ID: {}", transactionId);
    }

    @Override
    @Auditable(action = "RESTORE_BUDGET", module = "SYSTEM_RECOVERY", entityName = "budgets")
    public void restoreBudget(Long budgetId, String adminUsername) {
        log.info("Admin [{}] khôi phục ngân sách ID: {}", adminUsername, budgetId);

        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new BusinessException("BUDGET_NOT_FOUND", "Không tìm thấy ngân sách ID: " + budgetId));

        if (!Boolean.TRUE.equals(budget.getIsDeleted())) {
            throw new BusinessException("NOT_DELETED", "Bản ghi không ở trạng thái bị xóa");
        }

        budget.setIsDeleted(false);
        budget.setIsActive(true);
        budgetRepository.save(budget);
        log.info("Khôi phục thành công ngân sách ID: {}", budgetId);
    }

    @Override
    @Auditable(action = "RESTORE_SAVING_GOAL", module = "SYSTEM_RECOVERY", entityName = "saving_goals")
    public void restoreSavingGoal(Long goalId, String adminUsername) {
        log.info("Admin [{}] khôi phục mục tiêu tiết kiệm ID: {}", adminUsername, goalId);

        SavingGoal goal = savingGoalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "Không tìm thấy mục tiêu tiết kiệm ID: " + goalId));

        if (!Boolean.TRUE.equals(goal.getIsDeleted())) {
            throw new BusinessException("NOT_DELETED", "Bản ghi không ở trạng thái bị xóa");
        }

        goal.setIsDeleted(false);
        SavingGoalStatus restoredStatus = (goal.getCurrentAmount() != null
                && goal.getTargetAmount() != null
                && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0)
                ? SavingGoalStatus.ACHIEVED
                : SavingGoalStatus.IN_PROGRESS;
        goal.setStatus(restoredStatus);
        savingGoalRepository.save(goal);
        log.info("Khôi phục thành công mục tiêu tiết kiệm ID: {}", goalId);
    }
}
