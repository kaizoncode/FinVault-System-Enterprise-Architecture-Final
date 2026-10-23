package com.finvault.module2_wallet_ledger.entity;

import com.finvault.module1_security_rbac.entity.User;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * Category Entity: Đại diện cho các danh mục thu chi trong hệ thống
 *
 * Phân cấp danh mục:
 * - Danh mục hệ thống mặc định (isSystemDefault = true, user = null): Dành cho toàn bộ người dùng
 * - Danh mục tùy chỉnh cá nhân (isSystemDefault = false, user != null): Do từng người dùng tự tạo
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ cây danh mục đa cấp (Parent-Child Subcategories: Ăn uống -> Nhà hàng / Đi chợ)
 * ==============================================================================
 */
@Entity
@Table(name = "categories")
@EntityListeners(AuditingEntityListener.class)
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false, length = 20)
    private CategoryType categoryType;

    @Column(name = "icon", length = 50)
    private String icon = "bi-tag";

    @Column(name = "color", length = 20)
    private String color = "#0d6efd";

    @Column(name = "is_system_default", nullable = false)
    private Boolean isSystemDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    public Category() {
    }

    public Category(User user, String categoryName, CategoryType categoryType, String icon, String color, Boolean isSystemDefault) {
        this.user = user;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.icon = (icon != null && !icon.isBlank()) ? icon : "bi-tag";
        this.color = (color != null && !color.isBlank()) ? color : "#0d6efd";
        this.isSystemDefault = Boolean.TRUE.equals(isSystemDefault);
        this.isActive = true;
        this.isDeleted = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id != null && Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", categoryName='" + categoryName + '\'' +
                ", categoryType=" + categoryType +
                ", isSystemDefault=" + isSystemDefault +
                ", isActive=" + isActive +
                '}';
    }
}
