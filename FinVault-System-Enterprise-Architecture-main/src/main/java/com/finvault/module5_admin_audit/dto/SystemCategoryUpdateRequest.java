package com.finvault.module5_admin_audit.dto;

import com.finvault.module2_wallet_ledger.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemCategoryUpdateRequest: DTO quản trị thêm/sửa/ẩn danh mục gốc hệ thống từ thanh Live Edit Mode
 * ==============================================================================
 */
public class SystemCategoryUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String categoryName;

    @NotNull(message = "Loại danh mục không được để trống")
    private CategoryType categoryType;

    private String icon = "bi-tag";

    private String color = "#6c757d";

    private Boolean isActive = true;

    public SystemCategoryUpdateRequest() {
    }

    public SystemCategoryUpdateRequest(String categoryName, CategoryType categoryType, String icon, String color, Boolean isActive) {
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.icon = (icon != null) ? icon : "bi-tag";
        this.color = (color != null) ? color : "#6c757d";
        this.isActive = (isActive != null) ? isActive : true;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
