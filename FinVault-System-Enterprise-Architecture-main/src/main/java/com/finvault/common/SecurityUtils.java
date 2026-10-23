package com.finvault.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * SecurityUtils: Lớp tiện ích trích xuất ngữ cảnh bảo mật, phân quyền và che mặt nạ dữ liệu (Data Masking)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Context-aware dynamic ABAC (Attribute-Based Access Control)
 * ==============================================================================
 */
public final class SecurityUtils {

    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_SYSTEM_AUDITOR = "ROLE_SYSTEM_AUDITOR";
    public static final String ROLE_FAMILY_HEAD = "ROLE_FAMILY_HEAD";
    public static final String ROLE_STANDARD_USER = "ROLE_STANDARD_USER";

    private SecurityUtils() {
        // Utility class
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    public static boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSuperAdmin() {
        return hasRole(ROLE_SUPER_ADMIN);
    }

    public static boolean isSystemAuditor() {
        return hasRole(ROLE_SYSTEM_AUDITOR);
    }

    public static boolean isFamilyHead() {
        return hasRole(ROLE_FAMILY_HEAD);
    }

    public static boolean isStandardUser() {
        return hasRole(ROLE_STANDARD_USER);
    }

    /**
     * Che giấu số tiền nhạy cảm đối với tài khoản Kiểm toán viên (ROLE_SYSTEM_AUDITOR)
     */
    public static String formatCurrencyWithMasking(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0 " + (currency != null ? currency : "VND");
        }
        if (isSystemAuditor()) {
            return "***,*** " + (currency != null ? currency : "VND");
        }
        NumberFormat nf = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        return nf.format(amount) + " " + (currency != null ? currency : "VND");
    }

    /**
     * Che giấu chuỗi nhạy cảm (Số tài khoản, Ghi chú giao dịch)
     */
    public static String maskSensitiveString(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (raw.length() <= 4) {
            return "****";
        }
        int keep = Math.min(3, raw.length() / 3);
        String prefix = raw.substring(0, keep);
        String suffix = raw.substring(raw.length() - keep);
        return prefix + "*".repeat(raw.length() - (keep * 2)) + suffix;
    }

    /**
     * Trích xuất địa chỉ IP thực tế của client kể cả khi đi qua Proxy / Nginx / Vercel / Cloudflare
     */
    public static String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return getClientIpAddress(attributes.getRequest());
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    /**
     * Trích xuất địa chỉ IP thực tế từ HttpServletRequest cụ thể
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("CF-Connecting-IP"); // Cloudflare
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP"); // Nginx / Vercel
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && !ip.isBlank()) {
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
                ip = "127.0.0.1";
            }
            return ip;
        }
        return "127.0.0.1";
    }

    /**
     * Trích xuất User-Agent của client
     */
    public static String getClientUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return getClientUserAgent(attributes.getRequest());
            }
        } catch (Exception ignored) {
        }
        return "Unknown";
    }

    /**
     * Trích xuất User-Agent từ HttpServletRequest cụ thể
     */
    public static String getClientUserAgent(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ua = request.getHeader("User-Agent");
        return ua != null && !ua.isBlank() ? ua : "Unknown";
    }
}
