package com.finvault.module3_budget_goals.repository;

import com.finvault.module3_budget_goals.entity.LoanCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculationRepository: Tầng truy xuất dữ liệu Lịch sử tính toán khoản vay
 * ==============================================================================
 */
@Repository
public interface LoanCalculationRepository extends JpaRepository<LoanCalculation, Long> {

    Optional<LoanCalculation> findByIdAndIsDeletedFalse(Long id);

    List<LoanCalculation> findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsDeletedFalse(Long userId);
}
