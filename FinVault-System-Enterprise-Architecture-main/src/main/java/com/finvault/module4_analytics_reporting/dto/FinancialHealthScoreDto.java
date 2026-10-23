package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * FinancialHealthScoreDto: DTO cấu trúc điểm số và đánh giá sức khỏe tài chính toàn diện
 *
 * Tiêu chí đánh giá sức khỏe tài chính (Thang điểm 0 - 100):
 * 1. Tỷ lệ tiết kiệm (Savings Rate Component - Max 30 điểm): >= 20% đạt điểm tối đa.
 * 2. Tỷ lệ tuân thủ ngân sách (Budget Discipline Component - Max 25 điểm): Không vượt ngân sách.
 * 3. Tỷ lệ dự phòng khẩn cấp (Emergency Fund Ratio - Max 25 điểm): Đủ 3 - 6 tháng chi phí cơ bản.
 * 4. Tỷ lệ gánh nặng nợ (Debt-to-Income DTI - Max 20 điểm): Trả nợ vay < 30% thu nhập.
 * ==============================================================================
 */
public class FinancialHealthScoreDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalScore; // 0 - 100
    private String healthGrade; // EXCELLENT (>=85), GOOD (70-84), FAIR (50-69), POOR (<50)
    private String summaryAssessment;

    private BigDecimal savingsRate;
    private Integer savingsRateScore;

    private BigDecimal budgetAdherenceRate;
    private Integer budgetAdherenceScore;

    private BigDecimal emergencyFundMonths;
    private Integer emergencyFundScore;

    private BigDecimal debtToIncomeRatio;
    private Integer debtToIncomeScore;

    private List<String> strengthPoints = new ArrayList<>();
    private List<String> improvementRecommendations = new ArrayList<>();

    public FinancialHealthScoreDto() {
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public String getHealthGrade() {
        return healthGrade;
    }

    public void setHealthGrade(String healthGrade) {
        this.healthGrade = healthGrade;
    }

    public String getSummaryAssessment() {
        return summaryAssessment;
    }

    public void setSummaryAssessment(String summaryAssessment) {
        this.summaryAssessment = summaryAssessment;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }

    public Integer getSavingsRateScore() {
        return savingsRateScore;
    }

    public void setSavingsRateScore(Integer savingsRateScore) {
        this.savingsRateScore = savingsRateScore;
    }

    public BigDecimal getBudgetAdherenceRate() {
        return budgetAdherenceRate;
    }

    public void setBudgetAdherenceRate(BigDecimal budgetAdherenceRate) {
        this.budgetAdherenceRate = budgetAdherenceRate;
    }

    public Integer getBudgetAdherenceScore() {
        return budgetAdherenceScore;
    }

    public void setBudgetAdherenceScore(Integer budgetAdherenceScore) {
        this.budgetAdherenceScore = budgetAdherenceScore;
    }

    public BigDecimal getEmergencyFundMonths() {
        return emergencyFundMonths;
    }

    public void setEmergencyFundMonths(BigDecimal emergencyFundMonths) {
        this.emergencyFundMonths = emergencyFundMonths;
    }

    public Integer getEmergencyFundScore() {
        return emergencyFundScore;
    }

    public void setEmergencyFundScore(Integer emergencyFundScore) {
        this.emergencyFundScore = emergencyFundScore;
    }

    public BigDecimal getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(BigDecimal debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public Integer getDebtToIncomeScore() {
        return debtToIncomeScore;
    }

    public void setDebtToIncomeScore(Integer debtToIncomeScore) {
        this.debtToIncomeScore = debtToIncomeScore;
    }

    public List<String> getStrengthPoints() {
        return strengthPoints;
    }

    public void setStrengthPoints(List<String> strengthPoints) {
        this.strengthPoints = strengthPoints;
    }

    public List<String> getImprovementRecommendations() {
        return improvementRecommendations;
    }

    public void setImprovementRecommendations(List<String> improvementRecommendations) {
        this.improvementRecommendations = improvementRecommendations;
    }
}
