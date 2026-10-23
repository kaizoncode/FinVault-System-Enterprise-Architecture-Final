package com.finvault.module3_budget_goals.service;

import com.finvault.module3_budget_goals.dto.Budget50_30_20Request;
import com.finvault.module3_budget_goals.dto.BudgetRequest;
import com.finvault.module3_budget_goals.dto.BudgetResponseDto;
import com.finvault.module3_budget_goals.entity.Budget;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * BudgetService: Interface định nghĩa các nghiệp vụ Quản lý Ngân sách thông minh
 * ==============================================================================
 */
public interface BudgetService {

    BudgetResponseDto createBudget(String username, BudgetRequest request);

    List<BudgetResponseDto> create50_30_20Budgets(String username, Budget50_30_20Request request);

    BudgetResponseDto updateBudget(Long budgetId, String username, BudgetRequest request);

    void deleteBudget(Long budgetId, String username);

    BudgetResponseDto getBudgetById(Long budgetId, String username);

    Budget getBudgetEntityById(Long budgetId);

    List<BudgetResponseDto> getUserBudgets(String username);

    List<BudgetResponseDto> getActiveBudgets(String username);

    List<BudgetResponseDto> getStandardTemplates();

    BudgetResponseDto createStandardTemplate(String adminUsername, BudgetRequest request);
}
