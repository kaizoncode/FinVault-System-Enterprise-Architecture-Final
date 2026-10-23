package com.finvault.module2_wallet_ledger.dto;

import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.CategoryType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryResponseDto: DTO trả về thông tin danh mục thu chi
 * ==============================================================================
 */
public class CategoryResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String categoryName;
    private CategoryType categoryType;
    private String icon;
    private String color;
    private Boolean isSystemDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public CategoryResponseDto() {
    }

    public static CategoryResponseDto fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(category.getId());
        dto.setUserId(category.getUser() != null ? category.getUser().getId() : null);
        dto.setCategoryName(category.getCategoryName());
        dto.setCategoryType(category.getCategoryType());
        dto.setIcon(category.getIcon());
        dto.setColor(category.getColor());
        dto.setIsSystemDefault(category.getIsSystemDefault());
        dto.setIsActive(category.getIsActive());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
