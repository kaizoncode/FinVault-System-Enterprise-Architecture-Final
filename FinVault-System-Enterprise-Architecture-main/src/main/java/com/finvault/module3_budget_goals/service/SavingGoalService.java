package com.finvault.module3_budget_goals.service;

import com.finvault.module3_budget_goals.dto.SavingGoalDepositRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalRequest;
import com.finvault.module3_budget_goals.dto.SavingGoalResponseDto;
import com.finvault.module3_budget_goals.entity.SavingGoal;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalService: Interface định nghĩa các nghiệp vụ Quản lý Quỹ mục tiêu tiết kiệm
 * ==============================================================================
 */
public interface SavingGoalService {

    SavingGoalResponseDto createGoal(String username, SavingGoalRequest request);

    SavingGoalResponseDto updateGoal(Long goalId, String username, SavingGoalRequest request);

    void deleteGoal(Long goalId, String username);

    SavingGoalResponseDto getGoalById(Long goalId, String username);

    SavingGoal getGoalEntityById(Long goalId);

    List<SavingGoalResponseDto> getUserGoals(String username);

    List<SavingGoalResponseDto> getGoalsByStatus(String username, SavingGoalStatus status);

    SavingGoalResponseDto depositToGoal(Long goalId, String username, SavingGoalDepositRequest request);

    SavingGoalResponseDto withdrawFromGoal(Long goalId, String username, BigDecimal amount, Long targetWalletId);
}
