package com.finvault.module3_budget_goals.service;

import com.finvault.module3_budget_goals.dto.LoanCalculationRequest;
import com.finvault.module3_budget_goals.dto.LoanCalculationResponseDto;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculatorService: Interface định nghĩa các nghiệp vụ tính toán kế hoạch trả nợ vay ngân hàng
 * ==============================================================================
 */
public interface LoanCalculatorService {

    LoanCalculationResponseDto calculateLoan(String username, LoanCalculationRequest request);

    List<LoanCalculationResponseDto> getUserLoanCalculations(String username);

    LoanCalculationResponseDto getLoanCalculationById(Long id, String username);

    void deleteLoanCalculation(Long id, String username);
}
