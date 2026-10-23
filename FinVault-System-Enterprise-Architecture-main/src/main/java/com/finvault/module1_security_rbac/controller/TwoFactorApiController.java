package com.finvault.module1_security_rbac.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.TwoFactorSetupDto;
import com.finvault.module1_security_rbac.dto.TwoFactorVerifyDto;
import com.finvault.module1_security_rbac.service.TwoFactorAuthService;
import com.finvault.module1_security_rbac.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * TwoFactorApiController: REST API quản lý thiết lập, kích hoạt và hủy bỏ bảo mật 2 lớp
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Push Notification 1-tap approve trên App điện thoại di động
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/2fa")
@PreAuthorize("isAuthenticated()")
public class TwoFactorApiController {

    private final TwoFactorAuthService twoFactorAuthService;
    private final UserService userService;

    public TwoFactorApiController(TwoFactorAuthService twoFactorAuthService, UserService userService) {
        this.twoFactorAuthService = twoFactorAuthService;
        this.userService = userService;
    }

    /**
     * Khởi tạo quá trình bật 2FA: Trả về Secret Key, QR Code URL và Scratch Codes
     */
    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupDto>> initiateSetup() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        TwoFactorSetupDto setupDto = twoFactorAuthService.initiate2FaSetup(currentUsername);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo cấu hình 2FA thành công", setupDto));
    }

    /**
     * Xác nhận mã TOTP đầu tiên từ Google Authenticator để chính thức kích hoạt 2FA
     */
    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<Void>> enable2Fa(@Valid @RequestBody TwoFactorVerifyDto request) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean success = twoFactorAuthService.confirmAndEnable2Fa(currentUsername, request.getCode());
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_TOTP", "Mã xác thực 2FA không chính xác"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt bảo mật 2 lớp (2FA) thành công!", null));
    }

    /**
     * Hủy kích hoạt bảo mật 2 lớp
     */
    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable2Fa(@Valid @RequestBody TwoFactorVerifyDto request) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        boolean success = twoFactorAuthService.disable2Fa(currentUsername, request.getCode());
        if (!success) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_TOTP", "Mã xác thực 2FA không chính xác để hủy kích hoạt"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã tắt bảo mật 2 lớp (2FA) thành công.", null));
    }

    /**
     * Kiểm tra trạng thái kích hoạt 2FA hiện tại của tài khoản
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getStatus() {
        String currentUsername = SecurityUtils.getCurrentUsername();
        var user = userService.getUserByUsername(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(Map.of("isTwoFactorEnabled", user.getIsTwoFactorEnabled())));
    }
}
