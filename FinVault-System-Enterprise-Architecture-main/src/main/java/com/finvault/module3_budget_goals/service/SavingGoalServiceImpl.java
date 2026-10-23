package com.finvault.module3_budget_goals.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.TransferDirection;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.event.TransactionCreatedEvent;
import com.finvault.module2_wallet_ledger.repository.TransactionRepository;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module3_budget_goals.dto.SavingGoalDepositRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalResponseDto;
import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.SavingGoal;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;
import com.finvault.module3_budget_goals.repository.BudgetRepository;
import com.finvault.module3_budget_goals.repository.SavingGoalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalServiceImpl: Triển khai toàn bộ nghiệp vụ Quản lý Quỹ mục tiêu tiết kiệm
 *
 * Tính năng chống Race Condition & ACID:
 * - Khi nạp tiền từ ví vào quỹ mục tiêu hoặc rút tiền ngược lại, sử dụng Pessimistic Lock
 *   (findByIdForUpdate) để đảm bảo tính toàn vẹn số dư ví không bao giờ bị Race Condition.
 * - Tự động cập nhật trạng thái mục tiêu (IN_PROGRESS -> ACHIEVED) khi số tiền tích lũy chạm đích.
 * ==============================================================================
 */
@Service
@Transactional
public class SavingGoalServiceImpl implements SavingGoalService {

    private static final Logger log = LoggerFactory.getLogger(SavingGoalServiceImpl.class);

    private final SavingGoalRepository savingGoalRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BudgetRepository budgetRepository;

    public SavingGoalServiceImpl(SavingGoalRepository savingGoalRepository,
                                  UserRepository userRepository,
                                  WalletRepository walletRepository,
                                  TransactionRepository transactionRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this(savingGoalRepository, userRepository, walletRepository, transactionRepository, eventPublisher, null);
    }

    @Autowired
    public SavingGoalServiceImpl(SavingGoalRepository savingGoalRepository,
                                  UserRepository userRepository,
                                  WalletRepository walletRepository,
                                  TransactionRepository transactionRepository,
                                  ApplicationEventPublisher eventPublisher,
                                  BudgetRepository budgetRepository) {
        this.savingGoalRepository = savingGoalRepository;
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public SavingGoalResponseDto createGoal(String username, SavingGoalRequest request) {
        log.info("Khởi tạo Quỹ mục tiêu tiết kiệm mới cho user: '{}', mục tiêu: '{}'", username, request.getGoalName());
        validateTargetDate(request.getTargetDate());

        User user = getUserByUsername(username);

        Wallet wallet = null;
        BigDecimal initialAmount = (request.getInitialAmount() != null) ? request.getInitialAmount() : BigDecimal.ZERO;

        if (initialAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp ban đầu không được âm");
        }

        if (request.getWalletId() != null) {
            // Khóa ví để trích tiền ban đầu an toàn nếu initialAmount > 0
            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                wallet = walletRepository.findByIdForUpdate(request.getWalletId())
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví với ID: " + request.getWalletId()));
                validateWalletAccess(wallet, user);
                wallet.withdraw(initialAmount);
                walletRepository.save(wallet);
                log.info("Đã trích {} từ ví ID: {} nạp vào mục tiêu ban đầu", initialAmount, wallet.getId());
            } else {
                wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId())
                        .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví với ID: " + request.getWalletId()));
                validateWalletAccess(wallet, user);
            }
        } else if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("WALLET_REQUIRED", "Cần chỉ định ví khi có số tiền nạp ban đầu");
        }

        SavingGoal goal = new SavingGoal(
                user,
                wallet,
                request.getGoalName(),
                request.getTargetAmount(),
                initialAmount,
                request.getTargetDate(),
                request.getNotes()
        );

        SavingGoal saved = savingGoalRepository.save(goal);

        if (initialAmount.compareTo(BigDecimal.ZERO) > 0 && wallet != null) {
            // Ghi nhận giao dịch sổ cái dưới dạng TRANSFER (OUTGOING) để không bị tính nhầm là chi tiêu tiêu dùng
            Transaction tx = new Transaction(
                    wallet,
                    user,
                    null,
                    TransactionType.TRANSFER,
                    initialAmount,
                    LocalDateTime.now(),
                    "[SAVING_DEPOSIT:#" + saved.getId() + "] Nạp ban đầu vào mục tiêu: " + saved.getGoalName()
            );
            tx.setTransferDirection(TransferDirection.OUTGOING);
            tx.setApprovalStatus(ApprovalStatus.APPROVED);
            Transaction savedTx = transactionRepository.save(tx);

            updateSavingsAllocationBudgets(user.getId(), initialAmount, true);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
            }
        }

        log.info("Khởi tạo thành công Quỹ mục tiêu tiết kiệm ID: {}", saved.getId());
        return SavingGoalResponseDto.fromEntity(saved);
    }

    @Override
    public SavingGoalResponseDto updateGoal(Long goalId, String username, SavingGoalRequest request) {
        log.info("Cập nhật Quỹ mục tiêu tiết kiệm ID: {} bởi user: '{}'", goalId, username);
        validateTargetDate(request.getTargetDate());

        SavingGoal goal = getGoalEntityById(goalId);
        validateGoalOwnership(goal, username);

        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            throw new BusinessException("GOAL_ALREADY_CANCELLED", "Không thể cập nhật mục tiêu tiết kiệm đã bị hủy");
        }

        if (request.getWalletId() != null) {
            Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(request.getWalletId())
                    .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví với ID: " + request.getWalletId()));
            validateWalletAccess(wallet, goal.getUser());
            goal.setWallet(wallet);
        } else {
            goal.setWallet(null);
        }

        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setNotes(request.getNotes());

        // Kiểm tra lại trạng thái hoàn thành
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingGoalStatus.ACHIEVED);
        } else {
            goal.setStatus(SavingGoalStatus.IN_PROGRESS);
        }

        SavingGoal updated = savingGoalRepository.save(goal);
        log.info("Cập nhật hoàn tất Quỹ mục tiêu tiết kiệm ID: {}", updated.getId());
        return SavingGoalResponseDto.fromEntity(updated);
    }

    @Override
    public void deleteGoal(Long goalId, String username) {
        log.info("Hủy / Xóa Quỹ mục tiêu tiết kiệm ID: {} bởi user: '{}'", goalId, username);
        SavingGoal goal = getGoalEntityById(goalId);
        validateGoalOwnership(goal, username);

        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            log.warn("Mục tiêu tiết kiệm ID: {} đã bị hủy trước đó", goalId);
            return;
        }

        // Nếu mục tiêu còn số dư > 0, hoàn tiền về ví của mục tiêu
        BigDecimal refundAmount = goal.getCurrentAmount();
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Wallet refundWallet = null;
            if (goal.getWallet() != null && goal.getWallet().getId() != null) {
                refundWallet = walletRepository.findByIdForUpdate(goal.getWallet().getId())
                        .orElse(null);
            }
            if (refundWallet == null) {
                // Fallback linh hoạt: tìm ví cá nhân hợp lệ của user nếu goal chưa liên kết ví
                List<Wallet> userWallets = walletRepository.findAllByUserIdAndIsDeletedFalse(goal.getUser().getId());
                if (!userWallets.isEmpty()) {
                    refundWallet = walletRepository.findByIdForUpdate(userWallets.get(0).getId())
                            .orElse(null);
                }
            }

            if (refundWallet == null) {
                throw new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví hợp lệ để hoàn trả số tiền còn lại trong mục tiêu");
            }

            validateWalletAccess(refundWallet, goal.getUser());
            refundWallet.deposit(refundAmount);
            walletRepository.save(refundWallet);

            // Ghi nhận Transaction hoàn tiền dưới dạng TRANSFER (INCOMING)
            Transaction refundTx = new Transaction(
                    refundWallet,
                    goal.getUser(),
                    null,
                    TransactionType.TRANSFER,
                    refundAmount,
                    LocalDateTime.now(),
                    "[SAVING_WITHDRAW:#" + goal.getId() + "] Hoàn tiền khi hủy mục tiêu: " + goal.getGoalName()
            );
            refundTx.setTransferDirection(TransferDirection.INCOMING);
            refundTx.setApprovalStatus(ApprovalStatus.APPROVED);
            Transaction savedTx = transactionRepository.save(refundTx);

            updateSavingsAllocationBudgets(goal.getUser().getId(), refundAmount, false);

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
            }

            goal.withdraw(refundAmount);
            log.info("Đã hoàn {} từ mục tiêu ID: {} về ví ID: {}", refundAmount, goalId, refundWallet.getId());
        }

        goal.setIsDeleted(true);
        goal.setStatus(SavingGoalStatus.CANCELLED);
        savingGoalRepository.save(goal);
        log.info("Đã xóa mềm mục tiêu tiết kiệm ID: {}", goalId);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGoalResponseDto getGoalById(Long goalId, String username) {
        SavingGoal goal = getGoalEntityById(goalId);
        validateGoalOwnership(goal, username);
        return SavingGoalResponseDto.fromEntity(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGoal getGoalEntityById(Long goalId) {
        return savingGoalRepository.findByIdAndIsDeletedFalse(goalId)
                .orElseThrow(() -> new BusinessException("GOAL_NOT_FOUND", "Không tìm thấy mục tiêu tiết kiệm với ID: " + goalId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGoalResponseDto> getUserGoals(String username) {
        User user = getUserByUsername(username);
        return savingGoalRepository.findAllByUserIdAndIsDeletedFalse(user.getId()).stream()
                .map(SavingGoalResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGoalResponseDto> getGoalsByStatus(String username, SavingGoalStatus status) {
        User user = getUserByUsername(username);
        return savingGoalRepository.findAllByUserIdAndStatusAndIsDeletedFalse(user.getId(), status).stream()
                .map(SavingGoalResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SavingGoalResponseDto depositToGoal(Long goalId, String username, SavingGoalDepositRequest request) {
        log.info("Nạp tiền vào Quỹ mục tiêu ID: {} từ ví ID: {}, số tiền: {}", goalId, request.getSourceWalletId(), request.getAmount());
        SavingGoal goal = getGoalEntityById(goalId);
        validateGoalOwnership(goal, username);

        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            throw new BusinessException("GOAL_ALREADY_CANCELLED", "Không thể nạp tiền vào mục tiêu tiết kiệm đã bị hủy");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền nạp vào mục tiêu tiết kiệm phải lớn hơn 0");
        }

        // Khóa ví nguồn bằng Pessimistic Lock nhằm chống Race Condition
        Wallet sourceWallet = walletRepository.findByIdForUpdate(request.getSourceWalletId())
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví nguồn với ID: " + request.getSourceWalletId()));
        validateWalletAccess(sourceWallet, goal.getUser());

        // Trích tiền từ ví và nạp vào quỹ
        sourceWallet.withdraw(request.getAmount());
        goal.deposit(request.getAmount());

        // Tự động gán ví nếu mục tiêu chưa có ví liên kết mặc định
        if (goal.getWallet() == null) {
            goal.setWallet(sourceWallet);
        }

        walletRepository.save(sourceWallet);
        SavingGoal updatedGoal = savingGoalRepository.save(goal);

        // Ghi nhận Transaction dưới dạng TRANSFER (OUTGOING) để không làm sai lệch chi tiêu tiêu dùng
        Transaction tx = new Transaction(
                sourceWallet,
                goal.getUser(),
                null,
                TransactionType.TRANSFER,
                request.getAmount(),
                LocalDateTime.now(),
                "[SAVING_DEPOSIT:#" + updatedGoal.getId() + "] Nạp tiền vào mục tiêu: " + goal.getGoalName()
        );
        tx.setTransferDirection(TransferDirection.OUTGOING);
        tx.setApprovalStatus(ApprovalStatus.APPROVED);
        Transaction savedTx = transactionRepository.save(tx);

        updateSavingsAllocationBudgets(goal.getUser().getId(), request.getAmount(), true);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
        }

        log.info("Nạp tiền thành công vào Quỹ ID: {}. Tiến độ hiện tại: {}%", updatedGoal.getId(), updatedGoal.getProgressPercentage());
        return SavingGoalResponseDto.fromEntity(updatedGoal);
    }

    @Override
    public SavingGoalResponseDto withdrawFromGoal(Long goalId, String username, BigDecimal amount, Long targetWalletId) {
        log.info("Rút tiền từ Quỹ mục tiêu ID: {}, số tiền: {}, ví nhận: {}", goalId, amount, targetWalletId);
        SavingGoal goal = getGoalEntityById(goalId);
        validateGoalOwnership(goal, username);

        if (goal.getStatus() == SavingGoalStatus.CANCELLED) {
            throw new BusinessException("GOAL_ALREADY_CANCELLED", "Mục tiêu tiết kiệm đã bị hủy");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "Số tiền rút phải lớn hơn 0");
        }

        // Fallback: nếu targetWalletId == null, sử dụng goal.getWallet()
        final Long effectiveWalletId;
        if (targetWalletId != null) {
            effectiveWalletId = targetWalletId;
        } else if (goal.getWallet() != null && goal.getWallet().getId() != null) {
            effectiveWalletId = goal.getWallet().getId();
        } else {
            throw new BusinessException("WALLET_REQUIRED", "Không tìm thấy ví nhận tiền (cần chỉ định ví nhận hoặc mục tiêu phải liên kết sẵn ví)");
        }

        // Khóa ví nhận tiền bằng Pessimistic Lock
        Wallet targetWallet = walletRepository.findByIdForUpdate(effectiveWalletId)
                .orElseThrow(() -> new BusinessException("WALLET_NOT_FOUND", "Không tìm thấy ví đích với ID: " + effectiveWalletId));
        validateWalletAccess(targetWallet, goal.getUser());

        // Rút tiền khỏi Quỹ (method withdraw kiểm tra số dư hiện có)
        goal.withdraw(amount);

        // Kiểm tra tính nhất quán trạng thái: Nếu mục tiêu đã ACHIEVED mà bị rút tiền xuống dưới mức mục tiêu, chuyển về IN_PROGRESS [L-M1]
        if (goal.getStatus() == SavingGoalStatus.ACHIEVED && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) < 0) {
            goal.setStatus(SavingGoalStatus.IN_PROGRESS);
            log.warn("Mục tiêu tiết kiệm ID: [{}] đã bị rút tiền xuống dưới hạn mức mục tiêu (Hiện còn: {} / Cần: {}). Tự động chuyển trạng thái về IN_PROGRESS.",
                    goalId, goal.getCurrentAmount(), goal.getTargetAmount());
        }

        // Thực hiện cộng tiền an toàn vào ví
        targetWallet.deposit(amount);
        walletRepository.save(targetWallet);
        log.info("Đã hoàn {} về ví ID: {}", amount, effectiveWalletId);

        // Tạo Transaction với type=TRANSFER (INCOMING), note="[SAVING_WITHDRAW:#id] Rút từ mục tiêu: [goalName]"
        Transaction tx = new Transaction(
                targetWallet,
                goal.getUser(),
                null,
                TransactionType.TRANSFER,
                amount,
                LocalDateTime.now(),
                "[SAVING_WITHDRAW:#" + goal.getId() + "] Rút từ mục tiêu: " + goal.getGoalName()
        );
        tx.setTransferDirection(TransferDirection.INCOMING);
        tx.setApprovalStatus(ApprovalStatus.APPROVED);
        Transaction savedTx = transactionRepository.save(tx);

        updateSavingsAllocationBudgets(goal.getUser().getId(), amount, false);

        if (eventPublisher != null) {
            eventPublisher.publishEvent(new TransactionCreatedEvent(this, savedTx));
        }

        SavingGoal updatedGoal = savingGoalRepository.save(goal);
        return SavingGoalResponseDto.fromEntity(updatedGoal);
    }

    private void updateSavingsAllocationBudgets(Long userId, BigDecimal amount, boolean isDeposit) {
        if (budgetRepository == null || userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            List<Budget> savingsBudgets = budgetRepository.findActiveBudgetsByAllocationTypeAndDate(
                    userId, AllocationType.SAVINGS, LocalDate.now());
            for (Budget b : savingsBudgets) {
                if (isDeposit) {
                    b.recordExpense(amount);
                } else {
                    b.revertExpense(amount);
                }
                budgetRepository.save(b);
            }
        } catch (Exception e) {
            log.warn("Không thể cập nhật ngân sách SAVINGS cho user ID {}: {}", userId, e.getMessage());
        }
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));
    }

    private void validateTargetDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new BusinessException("INVALID_TARGET_DATE", "Ngày dự kiến hoàn thành không được để trống");
        }
        if (targetDate.isBefore(LocalDate.now())) {
            throw new BusinessException("INVALID_TARGET_DATE", "Ngày dự kiến hoàn thành mục tiêu phải ở tương lai");
        }
    }

    private void validateGoalOwnership(SavingGoal goal, String username) {
        if (goal.getUser() == null || !goal.getUser().getUsername().equals(username)) {
            throw new BusinessException("ACCESS_DENIED", "Bạn không có quyền thao tác trên mục tiêu tiết kiệm này");
        }
    }

    private void validateWalletAccess(Wallet wallet, User user) {
        if (wallet.getUser() != null && !wallet.getUser().getId().equals(user.getId())) {
            boolean isMember = wallet.getMembers().stream()
                    .anyMatch(m -> m.getUser().getId().equals(user.getId()) && Boolean.TRUE.equals(m.getIsActive()));
            if (!isMember) {
                throw new BusinessException("WALLET_ACCESS_DENIED", "Bạn không có quyền thao tác trên ví ID: " + wallet.getId());
            }
        }
    }
}
