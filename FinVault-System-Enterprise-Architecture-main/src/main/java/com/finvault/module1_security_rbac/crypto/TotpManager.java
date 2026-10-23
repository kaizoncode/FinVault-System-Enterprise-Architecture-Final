package com.finvault.module1_security_rbac.crypto;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * TotpManager: Trình quản lý xác thực 2 lớp (2FA) theo thuật toán Time-based One-Time Password (RFC 6238)
 * Tương thích chuẩn với Google Authenticator, Microsoft Authenticator, Authy
 *
 * Tính năng chính:
 * - Khởi tạo mã bí mật ngẫu nhiên chuẩn Base32
 * - Tạo đường dẫn URI định dạng otpauth://totp/ phục vụ sinh mã QR Code
 * - Xác thực mã TOTP 6 chữ số với cửa sổ kiểm tra độ trễ (Time Window Tolerance)
 * - Sinh mã OTP dự phòng (Scratch Backup Codes) khi mất thiết bị
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ gửi mã OTP qua SMS / Email / Telegram Webhook song song với TOTP
 * ==============================================================================
 */
@Component
public class TotpManager {

    private static final Logger log = LoggerFactory.getLogger(TotpManager.class);

    private final String issuer;
    private final GoogleAuthenticator googleAuthenticator;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpManager(
            @Value("${finvault.security.totp.issuer:FinVault Enterprise}") String issuer,
            @Value("${finvault.security.totp.window-size:1}") int windowSize) {
        this.issuer = issuer;
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setWindowSize(windowSize)
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
        log.info("TotpManager khởi tạo thành công với Issuer: '{}', Window Size: {}", issuer, windowSize);
    }

    /**
     * Tạo mới một khóa bí mật 2FA Base32
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Tạo đường dẫn URI QR Code chuẩn RFC 6238
     */
    public String getQrCodeUrl(String username, String secretKey) {
        try {
            String encodedIssuer = URLEncoder.encode(this.issuer, StandardCharsets.UTF_8).replace("+", "%20");
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8).replace("+", "%20");
            return String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                    encodedIssuer, encodedUsername, secretKey, encodedIssuer
            );
        } catch (Exception e) {
            log.error("Lỗi khi sinh URL QR Code cho user [{}]: {}", username, e.getMessage());
            return "";
        }
    }

    /**
     * Xác thực mã 6 số TOTP
     */
    public boolean verifyCode(String secretKey, int verificationCode) {
        if (secretKey == null || secretKey.isBlank()) {
            return false;
        }
        try {
            return googleAuthenticator.authorize(secretKey, verificationCode);
        } catch (Exception e) {
            log.warn("Lỗi khi kiểm tra mã TOTP: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Xác thực chuỗi mã TOTP (an toàn, không crash khi truyền chuỗi không phải số)
     */
    public boolean verifyCode(String secretKey, String codeString) {
        if (codeString == null || codeString.isBlank()) {
            return false;
        }
        try {
            codeString = codeString.replaceAll("\\s+", "");
            int code = Integer.parseInt(codeString);
            return verifyCode(secretKey, code);
        } catch (NumberFormatException e) {
            log.warn("Mã TOTP nhập vào không hợp lệ (không phải số nguyên)");
            return false;
        }
    }

    /**
     * Sinh danh sách 5 mã dự phòng khẩn cấp (8 chữ số mỗi mã)
     */
    public List<String> generateScratchCodes() {
        List<String> codes = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            int code = 10000000 + secureRandom.nextInt(90000000);
            codes.add(String.valueOf(code));
        }
        return codes;
    }
}
