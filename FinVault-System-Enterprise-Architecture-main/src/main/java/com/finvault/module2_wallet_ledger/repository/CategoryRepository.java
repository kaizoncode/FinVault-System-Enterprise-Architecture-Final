package com.finvault.module2_wallet_ledger.repository;

import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryRepository: Tầng truy xuất dữ liệu Danh mục thu chi
 *
 * Tính năng chính:
 * - Lọc danh mục khả dụng cho người dùng: Danh mục mặc định hệ thống + Danh mục tự tạo
 * - Tra cứu theo CategoryType (INCOME / EXPENSE)
 * ==============================================================================
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndIsDeletedFalse(Long id);

    List<Category> findAllByIsDeletedFalse();

    List<Category> findAllByIsSystemDefaultTrueAndIsDeletedFalse();

    List<Category> findAllByUserIdAndIsDeletedFalse(Long userId);

    @Query("SELECT c FROM Category c WHERE (c.isSystemDefault = true OR c.user.id = :userId) AND c.isDeleted = false AND c.isActive = true ORDER BY c.categoryType, c.categoryName")
    List<Category> findAvailableCategoriesForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Category c WHERE (c.isSystemDefault = true OR c.user.id = :userId) AND c.categoryType = :type AND c.isDeleted = false AND c.isActive = true ORDER BY c.categoryName")
    List<Category> findAvailableCategoriesForUserAndType(@Param("userId") Long userId, @Param("type") CategoryType type);

    boolean existsByCategoryNameAndUserIdAndIsDeletedFalse(String categoryName, Long userId);

    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.categoryName) = LOWER(:categoryName) AND c.user IS NULL AND c.isDeleted = false")
    boolean existsByCategoryNameAndUserIsNullAndIsDeletedFalse(@Param("categoryName") String categoryName);
}
