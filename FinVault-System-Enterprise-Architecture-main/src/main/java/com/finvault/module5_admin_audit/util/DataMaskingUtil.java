package com.finvault.module5_admin_audit.util;

import java.math.BigDecimal;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * DataMaskingUtil: Tiện ích che mặt nạ dữ liệu nhạy cảm (Data Masking) cho tài khoản Kiểm toán (Auditor)
 *
 * Tiêu chuẩn bảo mật dữ liệu doanh nghiệp:
 * - Che số dư tài khoản & số tiền giao dịch: "***,*** VND"
 * - Che số tài khoản ngân hàng: Giữ 4 số đầu và 4 số cuối (Ví dụ: "1903****8901")
 * - Che địa chỉ Email: Giữ ký tự đầu và domain (Ví dụ: "u***1@example.com")
 * - Che số điện thoại: Giữ 3 số đầu và 3 số cuối (Ví dụ: "091****888")
 * ==============================================================================
 */
public final class DataMaskingUtil {

    private DataMaskingUtil() {
    }

    /**
     * Che số tiền nhạy cảm đối với tài khoản kiểm toán viên
     */
    public static String maskAmount(BigDecimal amount) {
        if (amount == null) {
            return "***,*** VND";
        }
        return "***,*** VND";
    }

    /**
     * Che số tài khoản ngân hàng (Giữ 4 số đầu và 4 số cuối)
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return "N/A";
        }
        String clean = accountNumber.trim();
        if (clean.length() <= 8) {
            return "****" + clean.substring(Math.max(0, clean.length() - 4));
        }
        String prefix = clean.substring(0, 4);
        String suffix = clean.substring(clean.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * Che địa chỉ Email
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.com";
        }
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    /**
     * Che số điện thoại
     */
    public static String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 7) {
            return "09******";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    /**
     * Che mặt nạ các trường số tiền, số dư và thông tin nhạy cảm trong chuỗi JSON / toString của Audit Log
     * dành cho vai trò ROLE_SYSTEM_AUDITOR
     */
    public static String maskSensitiveJsonPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return payload;
        }
        String masked = payload;
        // Che các trường số tiền trong toString() hoặc JSON: balance=..., amount=..., etc.
        masked = masked.replaceAll(
                "(?i)(balance|initialBalance|amount|allocatedAmount|spentAmount|targetAmount|currentAmount|spendingLimit|monthlySpendingLimit|principalAmount|monthlyPayment|totalInterest|totalPayable)(\\s*[:=]\\s*\"?)[0-9]+(?:\\.[0-9]+)?(\"?)",
                "$1$2***,***$3"
        );
        // Che các con số đi kèm đơn vị tiền tệ VND
        masked = masked.replaceAll("(?i)\\b[0-9]{4,}(?:\\.[0-9]+)?(\\s*VND)\\b", "***,***$1");
        // Che số tài khoản ngân hàng trong payload
        masked = masked.replaceAll(
                "(?i)(bankAccountNumber|accountNumber)(\\s*[:=]\\s*['\"]?)[0-9]{4,}(['\"]?)",
                "$1$2****$3"
        );
        // Che khóa bí mật / mật khẩu nếu có
        masked = masked.replaceAll(
                "(?i)(password|passwordHash|twoFactorSecret|pendingTwoFactorSecret|scratchCodes)(\\s*[:=]\\s*['\"]?)[^,;}\\s'\"]+(['\"]?)",
                "$1$2[MASKED]$3"
        );
        return masked;
    }
}
