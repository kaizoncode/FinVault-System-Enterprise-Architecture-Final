package com.finvault.module5_admin_audit.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module2_wallet_ledger.entity.Category;
import com.finvault.module2_wallet_ledger.repository.CategoryRepository;
import com.finvault.module5_admin_audit.dto.*;
import com.finvault.module5_admin_audit.service.SystemConfigService;
import com.finvault.module5_admin_audit.service.SystemNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * InPlaceAdminCmsController: REST API Quản trị trực tiếp trên màn hình chính (Live Admin Edit Mode)
 *
 * Tính năng chính (Dành riêng cho ROLE_SUPER_ADMIN):
 * - Cập nhật tức thì các tham số cấu hình hệ thống, ngưỡng cảnh báo, hạn mức phê duyệt gia đình
 * - Ban hành và kích hoạt banner thông báo chạy trực tiếp trên thanh tiêu đề
 * - Quản trị thêm, sửa, ẩn/hiện danh mục thu chi hệ thống (System Categories)
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/admin/cms")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class InPlaceAdminCmsController {

    private final SystemConfigService configService;
    private final SystemNotificationService notificationService;
    private final CategoryRepository categoryRepository;

    public InPlaceAdminCmsController(SystemConfigService configService,
                                     SystemNotificationService notificationService,
                                     CategoryRepository categoryRepository) {
        this.configService = configService;
        this.notificationService = notificationService;
        this.categoryRepository = categoryRepository;
    }

    // =========================================================================
    // 1. QUẢN TRỊ CẤU HÌNH THỜI GIAN THỰC (SYSTEM CONFIGURATIONS)
    // =========================================================================

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<SystemConfigResponseDto>>> getAllConfigs() {
        List<SystemConfigResponseDto> configs = configService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success("Danh sách cấu hình hệ thống", configs));
    }

    @GetMapping("/configs/group/{group}")
    public ResponseEntity<ApiResponse<List<SystemConfigResponseDto>>> getConfigsByGroup(@PathVariable String group) {
        List<SystemConfigResponseDto> configs = configService.getConfigsByGroup(group);
        return ResponseEntity.ok(ApiResponse.success("Cấu hình nhóm: " + group, configs));
    }

    @PutMapping("/configs/{key}")
    public ResponseEntity<ApiResponse<SystemConfigResponseDto>> updateConfig(
            @PathVariable String key,
            @Valid @RequestBody SystemConfigRequest request) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        SystemConfigResponseDto updated = configService.updateConfig(key, adminUsername, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tham số cấu hình thành công", updated));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<SystemConfigResponseDto>> createConfig(
            @Valid @RequestBody SystemConfigRequest request) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        SystemConfigResponseDto created = configService.createConfig(adminUsername, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo tham số cấu hình thành công", created));
    }

    @DeleteMapping("/configs/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(@PathVariable String key) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        configService.deleteConfig(key, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Xóa tham số cấu hình thành công", null));
    }

    // =========================================================================
    // 2. QUẢN TRỊ THÔNG BÁO & BANNER TRÊN MÀN HÌNH CHÍNH (SYSTEM NOTIFICATIONS)
    // =========================================================================

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<List<SystemNotificationResponseDto>>> getAllNotifications() {
        List<SystemNotificationResponseDto> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(ApiResponse.success("Danh sách thông báo hệ thống", notifications));
    }

    @PostMapping("/notifications")
    public ResponseEntity<ApiResponse<SystemNotificationResponseDto>> createNotification(
            @Valid @RequestBody SystemNotificationRequest request) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        SystemNotificationResponseDto response = notificationService.createNotification(adminUsername, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ban hành thông báo mới thành công", response));
    }

    @PutMapping("/notifications/{id}")
    public ResponseEntity<ApiResponse<SystemNotificationResponseDto>> updateNotification(
            @PathVariable Long id,
            @Valid @RequestBody SystemNotificationRequest request) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        SystemNotificationResponseDto response = notificationService.updateNotification(id, adminUsername, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông báo thành công", response));
    }

    @PatchMapping("/notifications/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleNotification(
            @PathVariable Long id,
            @RequestParam boolean active) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        notificationService.toggleNotificationStatus(id, adminUsername, active);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái hiển thị thông báo thành công", null));
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id) {
        String adminUsername = SecurityUtils.getCurrentUsername();
        notificationService.deleteNotification(id, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Xóa thông báo thành công", null));
    }

    // =========================================================================
    // 3. QUẢN TRỊ DANH MỤC GỐC HỆ THỐNG TRỰC TIẾP (SYSTEM CATEGORIES)
    // =========================================================================

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createSystemCategory(
            @Valid @RequestBody SystemCategoryUpdateRequest request) {
        Category category = new Category(
                null,
                request.getCategoryName(),
                request.getCategoryType(),
                request.getIcon(),
                request.getColor(),
                true // isSystemDefault
        );
        category.setIsActive(request.getIsActive());
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Khởi tạo danh mục gốc hệ thống thành công", saved));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Category>> updateSystemCategory(
            @PathVariable Long id,
            @Valid @RequestBody SystemCategoryUpdateRequest request) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục ID: " + id));

        category.setCategoryName(request.getCategoryName());
        category.setCategoryType(request.getCategoryType());
        category.setIcon(request.getIcon());
        category.setColor(request.getColor());
        category.setIsActive(request.getIsActive());

        Category updated = categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục gốc hệ thống thành công", updated));
    }

    @PatchMapping("/categories/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleSystemCategory(
            @PathVariable Long id,
            @RequestParam boolean active) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("CATEGORY_NOT_FOUND", "Không tìm thấy danh mục ID: " + id));

        category.setIsActive(active);
        categoryRepository.save(category);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái danh mục thành công", null));
    }
}
