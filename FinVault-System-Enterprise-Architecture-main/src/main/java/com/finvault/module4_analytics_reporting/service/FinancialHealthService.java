package com.finvault.module4_analytics_reporting.service;

import com.finvault.module4_analytics_reporting.dto.FinancialHealthScoreDto;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * FinancialHealthService: Interface tính toán và đánh giá Sức khỏe Tài chính toàn diện
 * ==============================================================================
 */
public interface FinancialHealthService {

    FinancialHealthScoreDto calculateFinancialHealth(String username);

    FinancialHealthScoreDto calculateFinancialHealthByUserId(Long userId);
}
