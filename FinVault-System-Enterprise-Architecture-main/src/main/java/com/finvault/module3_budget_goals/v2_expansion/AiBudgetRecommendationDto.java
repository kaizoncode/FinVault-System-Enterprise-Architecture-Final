package com.finvault.module3_budget_goals.v2_expansion;

import com.finvault.module3_budget_goals.entity.AllocationType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * [Extension Point - V2.0 Future Expansion]: AiBudgetRecommendationDto
 * DTO chứa kết quả đề xuất tối ưu hóa ngân sách và kế hoạch tài chính thông minh do AI sinh ra
 *
 * Lộ trình tích hợp v2.0:
 * - AI Deep Learning phân tích lịch sử chi tiêu thực tế của người dùng qua các tháng.
 * - Phát hiện các khoản chi lãng phí (anomalies / leakage).
 * - Đề xuất phân bổ ngân sách 50/30/20 được cá nhân hóa cao.
 * ==============================================================================
 */
public class AiBudgetRecommendationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private BigDecimal monthlyIncome;
    private BigDecimal recommendedNeeds;
    private BigDecimal recommendedWants;
    private BigDecimal recommendedSavings;
    private String rationale;
    private List<String> optimizationTips = new ArrayList<>();
    private List<CategoryBudgetSuggestion> categorySuggestions = new ArrayList<>();

    public AiBudgetRecommendationDto() {
    }

    public static class CategoryBudgetSuggestion implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long categoryId;
        private String categoryName;
        private AllocationType allocationType;
        private BigDecimal currentAverageSpent;
        private BigDecimal suggestedLimit;
        private String advice;

        public CategoryBudgetSuggestion() {
        }

        public CategoryBudgetSuggestion(Long categoryId, String categoryName, AllocationType allocationType,
                                       BigDecimal currentAverageSpent, BigDecimal suggestedLimit, String advice) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.allocationType = allocationType;
            this.currentAverageSpent = currentAverageSpent;
            this.suggestedLimit = suggestedLimit;
            this.advice = advice;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public AllocationType getAllocationType() {
            return allocationType;
        }

        public void setAllocationType(AllocationType allocationType) {
            this.allocationType = allocationType;
        }

        public BigDecimal getCurrentAverageSpent() {
            return currentAverageSpent;
        }

        public void setCurrentAverageSpent(BigDecimal currentAverageSpent) {
            this.currentAverageSpent = currentAverageSpent;
        }

        public BigDecimal getSuggestedLimit() {
            return suggestedLimit;
        }

        public void setSuggestedLimit(BigDecimal suggestedLimit) {
            this.suggestedLimit = suggestedLimit;
        }

        public String getAdvice() {
            return advice;
        }

        public void setAdvice(String advice) {
            this.advice = advice;
        }
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getRecommendedNeeds() {
        return recommendedNeeds;
    }

    public void setRecommendedNeeds(BigDecimal recommendedNeeds) {
        this.recommendedNeeds = recommendedNeeds;
    }

    public BigDecimal getRecommendedWants() {
        return recommendedWants;
    }

    public void setRecommendedWants(BigDecimal recommendedWants) {
        this.recommendedWants = recommendedWants;
    }

    public BigDecimal getRecommendedSavings() {
        return recommendedSavings;
    }

    public void setRecommendedSavings(BigDecimal recommendedSavings) {
        this.recommendedSavings = recommendedSavings;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public List<String> getOptimizationTips() {
        return optimizationTips;
    }

    public void setOptimizationTips(List<String> optimizationTips) {
        this.optimizationTips = optimizationTips;
    }

    public List<CategoryBudgetSuggestion> getCategorySuggestions() {
        return categorySuggestions;
    }

    public void setCategorySuggestions(List<CategoryBudgetSuggestion> categorySuggestions) {
        this.categorySuggestions = categorySuggestions;
    }
}
