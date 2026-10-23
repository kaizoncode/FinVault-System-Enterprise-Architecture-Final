package com.finvault.module1_security_rbac.controller;

import com.finvault.common.ApiResponse;
import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.BootstrapAdminRequest;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.RoleRepository;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * BootstrapController: Cung cấp endpoint khởi tạo / đổi mật khẩu ban đầu an toàn
 * cho Super Admin mà không cần hardcode mật khẩu trong data.sql ([C-2])
 * ==============================================================================
 */
@RestController
@RequestMapping("/api/admin/bootstrap")
public class BootstrapController {

    private static final Logger log = LoggerFactory.getLogger(BootstrapController.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    @Value("${finvault.bootstrap.token:}")
    private String configuredBootstrapToken;

    public BootstrapController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               PasswordEncoder passwordEncoder,
                               AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepository = auditLogRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> bootstrapSuperAdmin(
            @Valid @RequestBody BootstrapAdminRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = SecurityUtils.getClientIpAddress(httpRequest);
        String userAgent = SecurityUtils.getClientUserAgent(httpRequest);

        // 1. Kiểm tra nếu hệ thống đã có tài khoản Super Admin thì CHẶN hoàn toàn
        boolean superAdminExists = userRepository.existsByRoleName(Role.ROLE_SUPER_ADMIN);
        if (superAdminExists) {
            log.warn("Cảnh báo bảo mật: Nỗ lực truy cập Bootstrap khi hệ thống đã có Super Admin từ IP: {}", clientIp);
            throw new BusinessException("BOOTSTRAP_ALREADY_COMPLETED", "Hệ thống đã hoàn tất khởi tạo Super Admin. Endpoint bootstrap đã bị vô hiệu hóa.", HttpStatus.FORBIDDEN);
        }

        // 2. Nếu chưa có Super Admin, BẮT BUỘC token cấu hình và token gửi lên phải khớp (không được để trống)
        if (configuredBootstrapToken == null || configuredBootstrapToken.isBlank()) {
            log.error("Cảnh báo bảo mật: FINVAULT_BOOTSTRAP_TOKEN chưa được cấu hình trên server.");
            throw new BusinessException("BOOTSTRAP_TOKEN_NOT_CONFIGURED", "Hệ thống chưa cấu hình Token khởi tạo bảo mật (FINVAULT_BOOTSTRAP_TOKEN)", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (request.getSetupToken() == null || !configuredBootstrapToken.equals(request.getSetupToken().trim())) {
            log.warn("Nỗ lực khởi tạo admin với Token không hợp lệ từ IP: {}", clientIp);
            throw new BusinessException("INVALID_BOOTSTRAP_TOKEN", "Token khởi tạo hệ thống không chính xác hoặc đã hết hạn", HttpStatus.FORBIDDEN);
        }

        String username = (request.getUsername() != null && !request.getUsername().isBlank())
                ? request.getUsername().trim()
                : "admin";

        Optional<User> existingUserOpt = userRepository.findByUsername(username);

        User adminUser;
        if (existingUserOpt.isPresent()) {
            adminUser = existingUserOpt.get();
            adminUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            adminUser.setFailedLoginAttempts(0);
            adminUser.setLockedUntil(null);
            adminUser.setIsActive(true);
            adminUser.setIsDeleted(false);
            log.info("Cập nhật lại mật khẩu khởi tạo an toàn cho tài khoản Quản trị: {}", username);
        } else {
            adminUser = new User();
            adminUser.setUsername(username);
            adminUser.setEmail(username + "@finvault.com");
            adminUser.setFullName("Super Administrator");
            adminUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            adminUser.setIsActive(true);
            adminUser.setIsDeleted(false);
            adminUser.setIsTwoFactorEnabled(false);

            Role superAdminRole = roleRepository.findByRoleName(Role.ROLE_SUPER_ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(Role.ROLE_SUPER_ADMIN, "Super Administrator")));
            adminUser.addRole(superAdminRole);
            log.info("Khởi tạo mới tài khoản Super Admin thành công: {}", username);
        }

        userRepository.save(adminUser);

        // Ghi vết kiểm toán Audit Trail
        try {
            AuditLog auditLog = new AuditLog(
                    username,
                    "ROLE_SUPER_ADMIN",
                    "ADMIN_BOOTSTRAP",
                    "IAM_SECURITY",
                    "app_users",
                    adminUser.getId(),
                    null,
                    "Khởi tạo / Thiết lập mật khẩu quản trị ban đầu thành công",
                    clientIp,
                    userAgent,
                    "SUCCESS",
                    null
            );
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Không thể ghi log audit khởi tạo admin: {}", ex.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Khởi tạo thông tin Super Admin thành công. Bạn có thể đăng nhập ngay.", username));
    }
}
