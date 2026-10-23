package com.finvault.module1_security_rbac.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * CryptoConfigValidator: Kiểm tra tính hợp lệ của khóa bí mật mã hóa AES-256
 * ngay khi ứng dụng khởi động (Fail-fast Security Validation)
 * ==============================================================================
 */
@Component
public class CryptoConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CryptoConfigValidator.class);

    @Value("${finvault.crypto.aes-secret-key:}")
    private String aesKey;

    private final Environment environment;

    public CryptoConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("production")
                || Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (aesKey == null || aesKey.isBlank()) {
            if (isProduction) {
                log.error("❌ LỖI BẢO MẬT NGHIÊM TRỌNG: FINVAULT_AES_KEY chưa được cấu hình trên môi trường Production!");
                throw new IllegalStateException("❌ FINVAULT_AES_KEY chưa được cấu hình! Hệ thống không thể khởi động trên Production.");
            } else {
                log.warn("⚠️ CẢNH BÁO BẢO MẬT: Biến môi trường FINVAULT_AES_KEY chưa được cấu hình. Hệ thống tự động tạo khóa 256-bit ngẫu nhiên trong bộ nhớ cho môi trường Dev.");
            }
            return;
        }

        String trimmedKey = aesKey.trim();
        if (trimmedKey.length() != 64) {
            log.error("❌ LỖI ĐỊNH DẠNG KHÓA: FINVAULT_AES_KEY có độ dài {} ký tự (yêu cầu chính xác 64 ký tự Hex cho khóa AES-256)", trimmedKey.length());
            throw new IllegalStateException("❌ FINVAULT_AES_KEY phải là 64 ký tự hex (chuẩn 256-bit AES)!");
        }

        if (!trimmedKey.matches("^[0-9a-fA-F]{64}$")) {
            log.error("❌ LỖI ĐỊNH DẠNG KHÓA: FINVAULT_AES_KEY chứa các ký tự không thuộc bảng mã Hexadecimal!");
            throw new IllegalStateException("❌ FINVAULT_AES_KEY chứa các ký tự không hợp lệ (chỉ chấp nhận 0-9, a-f, A-F)!");
        }

        log.info("✅ Xác thực cấu hình mã hóa thành công: FINVAULT_AES_KEY (256-bit GCM) đã sẵn sàng.");
    }
}
