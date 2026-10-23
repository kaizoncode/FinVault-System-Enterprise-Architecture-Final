package com.finvault.module4_analytics_reporting.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * CategoryExpenseDistributionDto: DTO cơ cấu phân bổ chi tiêu theo danh mục (Doughnut / Pie Chart)
 * ==============================================================================
 */
public class CategoryExpenseDistributionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal totalExpense = BigDecimal.ZERO;
    private List<CategoryExpenseItem> items = new ArrayList<>();

    public CategoryExpenseDistributionDto() {
    }

    public static class CategoryExpenseItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long categoryId;
        private String categoryName;
        private String icon;
        private String color;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO; // Ví dụ: 25.50%

        public CategoryExpenseItem() {
        }

        public CategoryExpenseItem(Long categoryId, String categoryName, String icon, String color,
                                   BigDecimal amount, BigDecimal percentage) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.icon = icon;
            this.color = color;
            this.amount = amount;
            this.percentage = percentage;
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

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public void setPercentage(BigDecimal percentage) {
            this.percentage = percentage;
        }
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public List<CategoryExpenseItem> getItems() {
        return items;
    }

    public void setItems(List<CategoryExpenseItem> items) {
        this.items = items;
    }
}
