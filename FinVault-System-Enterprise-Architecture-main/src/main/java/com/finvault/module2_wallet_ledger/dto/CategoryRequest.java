package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryRequest: DTO tạo hoặc cập nhật danh mục thu chi
 * ==============================================================================
 */
public class CategoryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String categoryName;

    @NotNull(message = "Loại danh mục (INCOME/EXPENSE) không được để trống")
    private CategoryType categoryType;

    private String icon = "bi-tag";

    private String color = "#0d6efd";

    private Boolean isSystemDefault = false;

    public CategoryRequest() {
    }

    public CategoryRequest(String categoryName, CategoryType categoryType, String icon, String color) {
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.icon = icon;
        this.color = color;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public CategoryType getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(CategoryType categoryType) {
        this.categoryType = categoryType;
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

    public Boolean getIsSystemDefault() {
        return isSystemDefault;
    }

    public void setIsSystemDefault(Boolean systemDefault) {
        isSystemDefault = systemDefault;
    }
}
