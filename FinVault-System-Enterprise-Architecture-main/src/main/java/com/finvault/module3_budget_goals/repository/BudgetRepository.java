package com.finvault.module3_budget_goals.repository;

import com.finvault.module3_budget_goals.entity.AllocationType;
import com.finvault.module3_budget_goals.entity.Budget;
import com.finvault.module3_budget_goals.entity.BudgetType;
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
 * BudgetRepository: Tầng truy xuất dữ liệu Ngân sách tài chính
 *
 * Tính năng chính:
 * - Lọc ngân sách khả dụng theo người dùng và khoảng thời gian hiệu lực
 * - Tra cứu ngân sách theo Danh mục để cập nhật tiến độ chi tiêu tức thì (Event-Driven)
 * - Tra cứu các Mẫu ngân sách chuẩn hệ thống (STANDARD_TEMPLATE)
 * - Các hàm tổng hợp phục vụ hiển thị biểu đồ và phân tích
 * ==============================================================================
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndIsDeletedFalse(Long id);

    List<Budget> findAllByUserIdAndIsDeletedFalse(Long userId);

    List<Budget> findAllByUserIdAndIsActiveTrueAndIsDeletedFalse(Long userId);

    List<Budget> findAllByBudgetTypeAndIsDeletedFalse(BudgetType budgetType);

    /**
     * Tra cứu các ngân sách đang có hiệu lực trong khoảng ngày cụ thể
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.isActive = true AND b.isDeleted = false " +
           "AND b.startDate <= :targetDate AND b.endDate >= :targetDate ORDER BY b.startDate DESC")
    List<Budget> findActiveBudgetsByDate(@Param("userId") Long userId, @Param("targetDate") LocalDate targetDate);

    /**
     * Tra cứu ngân sách áp dụng cho một Danh mục cụ thể tại thời điểm phát sinh giao dịch
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category.id = :categoryId " +
           "AND b.isActive = true AND b.isDeleted = false " +
           "AND b.startDate <= :txDate AND b.endDate >= :txDate")
    List<Budget> findActiveBudgetsByCategoryAndDate(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("txDate") LocalDate txDate);

    /**
     * Tra cứu ngân sách chung / ngân sách quy tắc 50/30/20 (không gán danh mục cụ thể: category IS NULL)
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category IS NULL " +
           "AND b.isActive = true AND b.startDate <= :date AND b.endDate >= :date AND b.isDeleted = false")
    List<Budget> findActiveBudgetsWithNullCategoryByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date);

    /**
     * Tra cứu ngân sách theo nhóm phân bổ 50/30/20 (NEEDS / WANTS / SAVINGS)
     */
    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.allocationType = :allocationType " +
           "AND b.isActive = true AND b.isDeleted = false " +
           "AND b.startDate <= :txDate AND b.endDate >= :txDate")
    List<Budget> findActiveBudgetsByAllocationTypeAndDate(
            @Param("userId") Long userId,
            @Param("allocationType") AllocationType allocationType,
            @Param("txDate") LocalDate txDate);

    /**
     * Tra cứu các ngân sách đã hết hạn (endDate < currentDate) nhưng vẫn đang active để Scheduler tự động rollover
     */
    @Query("SELECT b FROM Budget b WHERE b.endDate < :currentDate AND b.isActive = true AND b.isDeleted = false")
    List<Budget> findExpiredActiveBudgets(@Param("currentDate") LocalDate currentDate);

    /**
     * Tổng số tiền đã phân bổ ngân sách trong chu kỳ
     */
    @Query("SELECT COALESCE(SUM(b.allocatedAmount), 0.00) FROM Budget b WHERE (:userId IS NULL OR b.user.id = :userId) " +
           "AND b.isActive = true AND b.isDeleted = false AND b.startDate <= :date AND b.endDate >= :date")
    BigDecimal sumTotalAllocatedAmount(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * Tổng số tiền thực tế đã chi tiêu trong các ngân sách của chu kỳ
     */
    @Query("SELECT COALESCE(SUM(b.spentAmount), 0.00) FROM Budget b WHERE (:userId IS NULL OR b.user.id = :userId) " +
           "AND b.isActive = true AND b.isDeleted = false AND b.startDate <= :date AND b.endDate >= :date")
    BigDecimal sumTotalSpentAmount(@Param("userId") Long userId, @Param("date") LocalDate date);
}
