package com.finvault.module2_wallet_ledger.service;

import com.finvault.module2_wallet_ledger.dto.CategoryRequest;
import com.finvault.module2_wallet_ledger.dto.CategoryResponseDto;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.CategoryType;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryService: Interface định nghĩa các nghiệp vụ quản lý Danh mục thu chi
 * ==============================================================================
 */
public interface CategoryService {

    CategoryResponseDto createCategory(String username, CategoryRequest request);

    CategoryResponseDto updateCategory(Long categoryId, String username, CategoryRequest request);

    void deleteCategory(Long categoryId, String username);

    List<CategoryResponseDto> getAvailableCategories(String username);

    List<CategoryResponseDto> getAvailableCategoriesByType(String username, CategoryType type);

    CategoryResponseDto getCategoryById(Long categoryId);

    Category getCategoryEntityById(Long categoryId);
}
