package com.finvault.module3_budget_goals.repository;

import com.finvault.module3_budget_goals.entity.SavingGoal;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalRepository: Tầng truy xuất dữ liệu Quỹ mục tiêu tiết kiệm
 * ==============================================================================
 */
@Repository
public interface SavingGoalRepository extends JpaRepository<SavingGoal, Long> {

    Optional<SavingGoal> findByIdAndIsDeletedFalse(Long id);

    List<SavingGoal> findAllByUserIdAndIsDeletedFalse(Long userId);

    List<SavingGoal> findAllByUserIdAndStatusAndIsDeletedFalse(Long userId, SavingGoalStatus status);

    /**
     * Tra cứu các mục tiêu sắp đến hạn (trong vòng 30 ngày) để Scheduler gửi thông báo
     */
    @Query("SELECT g FROM SavingGoal g WHERE g.status = 'IN_PROGRESS' AND g.isDeleted = false " +
           "AND g.targetDate >= :today AND g.targetDate <= :upcomingDate ORDER BY g.targetDate ASC")
    List<SavingGoal> findUpcomingDueGoals(@Param("today") LocalDate today, @Param("upcomingDate") LocalDate upcomingDate);

    /**
     * Tổng số tiền mục tiêu cần tiết kiệm của người dùng
     */
    @Query("SELECT COALESCE(SUM(g.targetAmount), 0.00) FROM SavingGoal g WHERE g.user.id = :userId AND g.isDeleted = false")
    BigDecimal sumTotalTargetAmountByUserId(@Param("userId") Long userId);

    /**
     * Tổng số tiền thực tế đã tích lũy trong các quỹ mục tiêu
     */
    @Query("SELECT COALESCE(SUM(g.currentAmount), 0.00) FROM SavingGoal g WHERE g.user.id = :userId AND g.isDeleted = false")
    BigDecimal sumTotalCurrentAmountByUserId(@Param("userId") Long userId);
}
