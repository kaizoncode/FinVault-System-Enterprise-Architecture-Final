package com.finvault.module3_budget_goals.v2_expansion;

import java.math.BigDecimal;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * [Extension Point - V2.0 Future Expansion]: AiBudgetAdvisorProvider
 * Interface mở rộng tích hợp Cố vấn Tài chính AI thông minh (AI Financial Advisor)
 *
 * Lộ trình tích hợp v2.0:
 * 1. Phân tích hành vi chi tiêu thực tế dựa trên Big Data và Machine Learning.
 * 2. Đưa ra gợi ý phân bổ ngân sách 50/30/20 được cá nhân hóa theo từng gia đình.
 * 3. Cảnh báo sớm nguy cơ mất cân đối thu chi trước khi chạm ngưỡng 100%.
 * 4. Tự động đề xuất lịch trình trả nợ tối ưu (Debt Avalanche / Debt Snowball).
 * ==============================================================================
 */
public interface AiBudgetAdvisorProvider {

    /**
     * Phân tích lịch sử chi tiêu và đưa ra đề xuất ngân sách thông minh
     *
     * @param userId ID người dùng cần phân tích
     * @param monthlyIncome Thu nhập ước tính hàng tháng
     * @return DTO chứa các chỉ số và lời khuyên tối ưu ngân sách
     */
    AiBudgetRecommendationDto generateSmartBudgetPlan(Long userId, BigDecimal monthlyIncome);

    /**
     * Đánh giá rủi ro tài chính và nguy cơ vỡ ngân sách trong tháng hiện tại
     *
     * @param userId ID người dùng
     * @return Điểm rủi ro tài chính (Thang điểm 0 - 100, 0 là an toàn, 100 là cực kỳ nguy hiểm)
     */
    int assessBudgetRiskScore(Long userId);

    /**
     * Đưa ra danh sách các hành động cắt giảm chi phí thực tế có thể thực hiện ngay
     *
     * @param userId ID người dùng
     * @return Danh sách các gợi ý tiết kiệm cụ thể
     */
    List<String> suggestActionableCostCuts(Long userId);

    /**
     * Dự báo ngày hoàn thành khả thi của một mục tiêu tiết kiệm dựa trên tốc độ tích lũy thực tế
     *
     * @param goalId ID mục tiêu tiết kiệm
     * @return Số tháng ước tính cần thêm để đạt mục tiêu
     */
    int predictMonthsToAchieveGoal(Long goalId);
}
