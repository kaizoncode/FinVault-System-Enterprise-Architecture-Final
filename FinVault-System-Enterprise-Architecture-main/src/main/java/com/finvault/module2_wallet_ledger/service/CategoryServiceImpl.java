package com.finvault.module2_wallet_ledger.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module2_wallet_ledger.dto.CategoryRequest;
import com.finvault.module2_wallet_ledger.dto.CategoryResponseDto;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.CategoryType;
import com.finvault.module2_wallet_ledger.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryServiceImpl: Triển khai các nghiệp vụ quản lý Danh mục thu chi
 *
 * Kiểm soát quyền hạn:
 * - Super Admin: Toàn quyền thêm, sửa, xóa danh mục hệ thống mặc định (isSystemDefault = true)
 * - Người dùng thông thường: Chỉ được tạo, sửa, xóa danh mục cá nhân của chính mình
 * ==============================================================================
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, UserService userService) {
        this.categoryRepository = categoryRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public CategoryResponseDto createCategory(String username, CategoryRequest request) {
        User user = userService.getUserEntityByUsername(username);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);
        boolean createAsSystemDefault = Boolean.TRUE.equals(request.getIsSystemDefault()) && isSuperAdmin;

        boolean exists = createAsSystemDefault
                ? categoryRepository.existsByCategoryNameAndUserIsNullAndIsDeletedFalse(request.getCategoryName().trim())
                : categoryRepository.existsByCategoryNameAndUserIdAndIsDeletedFalse(request.getCategoryName().trim(), user.getId());

        if (exists) {
            throw new BusinessException("CATEGORY_EXISTS", "Danh mục '" + request.getCategoryName() + "' đã tồn tại");
        }

        Category category = new Category();
        category.setUser(createAsSystemDefault ? null : user);
        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryType(request.getCategoryType());
        category.setIcon(request.getIcon() != null ? request.getIcon().trim() : "bi-tag");
        category.setColor(request.getColor() != null ? request.getColor().trim() : "#0d6efd");
        category.setIsSystemDefault(createAsSystemDefault);
        category.setIsActive(true);
        category.setIsDeleted(false);

        Category saved = categoryRepository.save(category);
        log.info("Người dùng [{}] đã tạo danh mục mới: ID [{}], Tên [{}], Loại [{}]",
                username, saved.getId(), saved.getCategoryName(), saved.getCategoryType());
        return CategoryResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, String username, CategoryRequest request) {
        User user = userService.getUserEntityByUsername(username);
        Category category = getCategoryEntityById(categoryId);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);

        if (Boolean.TRUE.equals(category.getIsSystemDefault()) && !isSuperAdmin) {
            throw new BusinessException("FORBIDDEN", "Chỉ Super Admin mới có quyền chỉnh sửa danh mục hệ thống mặc định", HttpStatus.FORBIDDEN);
        }

        if (!Boolean.TRUE.equals(category.getIsSystemDefault()) && !category.getUser().getId().equals(user.getId()) && !isSuperAdmin) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền chỉnh sửa danh mục này", HttpStatus.FORBIDDEN);
        }

        category.setCategoryName(request.getCategoryName().trim());
        category.setCategoryType(request.getCategoryType());
        if (request.getIcon() != null) category.setIcon(request.getIcon().trim());
        if (request.getColor() != null) category.setColor(request.getColor().trim());

        Category updated = categoryRepository.save(category);
        log.info("Người dùng [{}] đã cập nhật danh mục [{}]: ID [{}]", username, updated.getCategoryName(), updated.getId());
        return CategoryResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, String username) {
        User user = userService.getUserEntityByUsername(username);
        Category category = getCategoryEntityById(categoryId);

        boolean isSuperAdmin = user.hasRole(Role.ROLE_SUPER_ADMIN);

        if (Boolean.TRUE.equals(category.getIsSystemDefault()) && !isSuperAdmin) {
            throw new BusinessException("FORBIDDEN", "Chỉ Super Admin mới có quyền xóa danh mục hệ thống mặc định", HttpStatus.FORBIDDEN);
        }

        if (!Boolean.TRUE.equals(category.getIsSystemDefault()) && !category.getUser().getId().equals(user.getId()) && !isSuperAdmin) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền xóa danh mục này", HttpStatus.FORBIDDEN);
        }

        category.setIsDeleted(true);
        category.setIsActive(false);
        categoryRepository.save(category);
        log.info("Người dùng [{}] đã xóa mềm danh mục [{}]: ID [{}]", username, category.getCategoryName(), category.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAvailableCategories(String username) {
        User user = userService.getUserEntityByUsername(username);
        return categoryRepository.findAvailableCategoriesForUser(user.getId()).stream()
                .map(CategoryResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAvailableCategoriesByType(String username, CategoryType type) {
        User user = userService.getUserEntityByUsername(username);
        return categoryRepository.findAvailableCategoriesForUserAndType(user.getId(), type).stream()
                .map(CategoryResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long categoryId) {
        return CategoryResponseDto.fromEntity(getCategoryEntityById(categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryEntityById(Long categoryId) {
        return categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục có ID: " + categoryId, HttpStatus.NOT_FOUND));
    }
}
