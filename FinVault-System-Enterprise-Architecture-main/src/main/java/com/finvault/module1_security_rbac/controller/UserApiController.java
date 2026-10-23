package com.finvault.module1_security_rbac.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.PasswordChangeDto;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserApiController: REST API quản trị thông tin cá nhân và quản trị người dùng (Live Admin CMS)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ upload ảnh đại diện (Avatar) lên S3 / MinIO Cloud Storage
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy thông tin hồ sơ của người dùng hiện tại đang đăng nhập
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentProfile() {
        String username = SecurityUtils.getCurrentUsername();
        UserResponseDto user = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", user));
    }

    /**
     * Đổi mật khẩu tài khoản cá nhân
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeDto dto) {
        String username = SecurityUtils.getCurrentUsername();
        userService.changePassword(username, dto);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    /**
     * [Dành cho Super Admin]: Xem danh sách toàn bộ người dùng trong hệ thống
     */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Danh sách người dùng", users));
    }

    /**
     * [Dành cho Super Admin & Auditor]: Xem danh sách các tài khoản đang bị khóa do nhập sai mật khẩu
     */
    @GetMapping("/locked")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_AUDITOR')")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getLockedUsers() {
        List<UserResponseDto> lockedUsers = userService.getLockedUsers();
        return ResponseEntity.ok(ApiResponse.success("Danh sách tài khoản đang bị khóa", lockedUsers));
    }

    /**
     * [Dành cho Super Admin]: Mở khóa tài khoản trực tiếp ngay trên màn hình chính
     */
    @PutMapping("/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable("id") Long id) {
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khóa tài khoản thành công!", null));
    }

    /**
     * [Dành cho Super Admin]: Khóa / Mở khóa hoạt động của người dùng
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> toggleUserStatus(
            @PathVariable("id") Long id,
            @RequestParam("active") boolean active) {
        UserResponseDto updated = userService.updateUserStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", updated));
    }
}
