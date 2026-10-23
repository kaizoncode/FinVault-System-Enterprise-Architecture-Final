package com.finvault.module2_wallet_ledger.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module2_wallet_ledger.dto.CategoryRequest;
import com.finvault.module2_wallet_ledger.dto.CategoryResponseDto;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.entity.CategoryType;
import com.finvault.module2_wallet_ledger.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 2 - Core Ledger & Multi-Wallet Module]
 * CategoryApiController: REST API quản trị Danh mục thu chi
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/categories")
@PreAuthorize("isAuthenticated()")
public class CategoryApiController {

    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryApiController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    /**
     * Lấy danh sách danh mục khả dụng cho người dùng hiện tại (Hệ thống + Cá nhân)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAvailableCategories() {
        String username = SecurityUtils.getCurrentUsername();
        List<CategoryResponseDto> categories = categoryService.getAvailableCategories(username);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục thành công", categories));
    }

    /**
     * Lọc danh mục theo loại (INCOME hoặc EXPENSE)
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getCategoriesByType(@PathVariable("type") CategoryType type) {
        String username = SecurityUtils.getCurrentUsername();
        List<CategoryResponseDto> categories = categoryService.getAvailableCategoriesByType(username, type);
        return ResponseEntity.ok(ApiResponse.success("Lọc danh mục theo loại thành công", categories));
    }

    /**
     * Lấy chi tiết một danh mục theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> getCategoryById(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        User currentUser = username != null ? userService.getUserEntityByUsername(username) : null;
        Category category = categoryService.getCategoryEntityById(id);

        boolean isAllowed = category.getUser() == null ||
                (currentUser != null && (currentUser.hasRole(Role.ROLE_SUPER_ADMIN) || category.getUser().getId().equals(currentUser.getId())));

        if (!isAllowed) {
            throw new BusinessException("FORBIDDEN", "Bạn không có quyền truy cập danh mục này", HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(ApiResponse.success("Chi tiết danh mục", CategoryResponseDto.fromEntity(category)));
    }

    /**
     * Tạo mới danh mục thu chi
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDto>> createCategory(@Valid @RequestBody CategoryRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        CategoryResponseDto created = categoryService.createCategory(username, request);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục mới thành công", created));
    }

    /**
     * Cập nhật thông tin danh mục
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        CategoryResponseDto updated = categoryService.updateCategory(id, username, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", updated));
    }

    /**
     * Xóa danh mục (Soft Delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") Long id) {
        String username = SecurityUtils.getCurrentUsername();
        categoryService.deleteCategory(id, username);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa danh mục thành công", null));
    }
}
