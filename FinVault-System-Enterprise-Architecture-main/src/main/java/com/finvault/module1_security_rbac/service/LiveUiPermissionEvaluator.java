package com.finvault.module1_security_rbac.service;

import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module5_admin_audit.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * LiveUiPermissionEvaluator: Bộ đánh giá quyền hạn trực tiếp trên giao diện chính (Live In-Place RBAC)
 * Được gọi trực tiếp từ Thymeleaf Templates (th:if="${@liveUiPermission.canAccessAdminLiveEdit(#authentication)}")
 * và từ các Controller/Service để kiểm soát quyền chỉnh sửa tại chỗ (In-Place Live Edit).
 *
 * Phân quyền 4 Roles trên Màn hình chính (/dashboard):
 * 1. ROLE_SUPER_ADMIN:
 *    - Bật thanh công cụ "Admin Live Edit Mode"
 *    - Chỉnh sửa trực tiếp banner thông báo, thêm/sửa/ẩn danh mục thu chi gốc
 *    - Điều chỉnh ngưỡng cảnh báo (80%/100%), mở khóa người dùng
 * 2. ROLE_SYSTEM_AUDITOR:
 *    - Hiển thị bảng giám sát bảo mật thời gian thực và Audit Trail
 *    - Kích hoạt chế độ Data Masking (che số dư cá nhân: '***,*** VND')
 * 3. ROLE_FAMILY_HEAD:
 *    - Hiển thị widget Quỹ chung gia đình và danh sách thành viên phụ
 *    - Thiết lập hạn mức chi tiêu cho thành viên gia đình
 *    - Phê duyệt / Từ chối các khoản chi lớn trực tiếp trên bảng giao dịch
 * 4. ROLE_STANDARD_USER:
 *    - Chỉ xem và thao tác trên dữ liệu ví và ngân sách của chính mình
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ phân quyền động theo thời gian thực (Time-based temporary privilege elevation)
 * ==============================================================================
 */
@Component("liveUiPermission")
public class LiveUiPermissionEvaluator {

    @Value("${finvault.family.approval-threshold-vnd:5000000}")
    private BigDecimal approvalThreshold = new BigDecimal("5000000");

    private final SystemConfigService systemConfigService;

    public LiveUiPermissionEvaluator() {
        this.systemConfigService = null;
    }

    @Autowired
    public LiveUiPermissionEvaluator(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    public BigDecimal getEffectiveApprovalThreshold() {
        BigDecimal fallback = (approvalThreshold != null) ? approvalThreshold : new BigDecimal("5000000");
        if (systemConfigService != null) {
            try {
                String val = systemConfigService.getConfigValue("FAMILY_EXPENSE_APPROVAL_THRESHOLD", fallback.toPlainString());
                if (val != null && !val.isBlank()) {
                    return new BigDecimal(val.trim());
                }
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || !authentication.isAuthenticated() || roleName == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (roleName.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Authentication getAuth(Authentication authentication) {
        if (authentication != null) {
            return authentication;
        }
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Quyền kích hoạt Admin Live Edit Mode trên màn hình chính
     */
    public boolean canAccessAdminLiveEdit(Authentication authentication) {
        return hasRole(getAuth(authentication), Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền sửa trực tiếp banner thông báo hệ thống
     */
    public boolean canEditSystemBanner(Authentication authentication) {
        return hasRole(getAuth(authentication), Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền thêm/sửa/ẩn danh mục gốc (System Categories)
     */
    public boolean canEditSystemCategories(Authentication authentication) {
        return hasRole(getAuth(authentication), Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền điều chỉnh ngưỡng cảnh báo hệ thống
     */
    public boolean canEditAlertThresholds(Authentication authentication) {
        return hasRole(getAuth(authentication), Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền mở khóa tài khoản người dùng bị khóa do brute-force
     */
    public boolean canUnlockUserAccounts(Authentication authentication) {
        return hasRole(getAuth(authentication), Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền xem bảng giám sát bảo mật và Audit Trail
     */
    public boolean canViewSecurityAuditWidget(Authentication authentication) {
        Authentication auth = getAuth(authentication);
        return hasRole(auth, Role.ROLE_SUPER_ADMIN) || hasRole(auth, Role.ROLE_SYSTEM_AUDITOR);
    }

    /**
     * Xác định tài khoản có bị kích hoạt Data Masking số dư (ROLE_SYSTEM_AUDITOR) hay không
     */
    public boolean shouldMaskFinancialAmounts(Authentication authentication) {
        Authentication auth = getAuth(authentication);
        // Auditor bị che số dư, Admin và User bình thường xem số thực
        return hasRole(auth, Role.ROLE_SYSTEM_AUDITOR) && !hasRole(auth, Role.ROLE_SUPER_ADMIN);
    }

    /**
     * Quyền quản trị Quỹ gia đình và đặt hạn mức chi
     */
    public boolean canAccessFamilyHeadControls(Authentication authentication) {
        Authentication auth = getAuth(authentication);
        return hasRole(auth, Role.ROLE_SUPER_ADMIN) || hasRole(auth, Role.ROLE_FAMILY_HEAD);
    }

    /**
     * Xác định khoản chi có vượt ngưỡng yêu cầu Family Head phê duyệt hay không
     */
    public boolean requiresFamilyApproval(BigDecimal amount) {
        BigDecimal threshold = getEffectiveApprovalThreshold();
        return amount != null && threshold != null && amount.compareTo(threshold) >= 0;
    }

    /**
     * Quyền duyệt / từ chối giao dịch chi tiêu của gia đình:
     * Người dùng có vai trò Family Head hoặc Super Admin có quyền phê duyệt các khoản chi
     */
    public boolean canApproveFamilyExpense(Authentication authentication, BigDecimal amount) {
        Authentication auth = getAuth(authentication);
        boolean hasHeadOrAdminRole = hasRole(auth, Role.ROLE_SUPER_ADMIN) || hasRole(auth, Role.ROLE_FAMILY_HEAD);
        if (!hasHeadOrAdminRole) {
            return false;
        }
        return amount == null || amount.compareTo(BigDecimal.ZERO) >= 0;
    }
}
