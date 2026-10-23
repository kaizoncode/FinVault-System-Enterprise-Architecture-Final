package com.finvault.module1_security_rbac.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * AesEncryptionConverter: JPA AttributeConverter thực hiện mã hóa và giải mã tự động
 * ở cấp độ ứng dụng (Application-level Encryption) sử dụng thuật toán chuẩn AES-256-GCM.
 *
 * Tính năng bảo mật cốt lõi:
 * - Khóa bí mật 256-bit (32 bytes Hex) cấu hình tập trung trong application.yml
 * - Chế độ GCM (Galois/Counter Mode) cung cấp cả tính bảo mật (Confidentiality)
 *   lẫn tính toàn vẹn (Integrity Authentication) chống sửa đổi dữ liệu (Tampering)
 * - Mỗi bản ghi mã hóa sinh một IV (Initialization Vector) 12 bytes ngẫu nhiên mới hoàn toàn
 * - Ghép IV + CipherText + Authentication Tag và mã hóa Base64 trước khi ghi vào Database
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Cloud KMS (AWS KMS, Azure Key Vault, HashiCorp Vault) để tự động xoay vòng khóa (Key Rotation)
 * ==============================================================================
 */
@Converter
@Component
public class AesEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionConverter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static byte[] staticKeyBytes;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Value("${finvault.system.environment:DEVELOPMENT}")
    private String environment = "DEVELOPMENT";

    public AesEncryptionConverter() {
        // KHÔNG BAO GIỜ fallback sang random key tạm thời trong bộ nhớ để tránh hỏng dữ liệu khi restart
    }

    @Value("${finvault.crypto.aes-secret-key:}")
    public void setSecretKey(String hexKey) {
        if (hexKey != null && !hexKey.isBlank()) {
            byte[] parsed = hexStringToByteArray(hexKey.trim());
            if (parsed.length == 32) {
                staticKeyBytes = parsed;
                log.info("AesEncryptionConverter: Khóa mã hóa AES-256 từ cấu hình đã được nạp thành công.");
                return;
            } else {
                throw new IllegalArgumentException("Khóa mã hóa FINVAULT_AES_KEY phải có đúng độ dài 256-bit (64 ký tự Hex).");
            }
        }

        // Bắt buộc trong môi trường PRODUCTION phải có biến môi trường FINVAULT_AES_KEY (Fail-fast)
        if ("PRODUCTION".equalsIgnoreCase(environment != null ? environment.trim() : "")) {
            throw new IllegalStateException("FINVAULT_AES_KEY environment variable is REQUIRED. Application cannot start without a persistent encryption key.");
        }

        // Môi trường DEVELOPMENT: Nạp hoặc tạo khóa cục bộ bền vững trên đĩa (./data/.finvault_aes.key)
        staticKeyBytes = getOrCreatePersistentLocalKey();
        log.warn("AesEncryptionConverter: Biến môi trường FINVAULT_AES_KEY chưa được đặt. Đang sử dụng khóa cục bộ bền vững tại './data/.finvault_aes.key' cho môi trường DEVELOPMENT.");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(getEffectiveKeyBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] messagePayload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, messagePayload, 0, iv.length);
            System.arraycopy(cipherText, 0, messagePayload, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(messagePayload);
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi thực hiện mã hóa AES-256-GCM cho trường dữ liệu nhạy cảm: {}", e.getMessage());
            throw new IllegalStateException("Không thể mã hóa dữ liệu nhạy cảm", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            byte[] messagePayload;
            try {
                messagePayload = Base64.getDecoder().decode(dbData);
            } catch (IllegalArgumentException notBase64) {
                // Hỗ trợ backward-compatibility nếu dữ liệu gốc là plain text
                return dbData;
            }

            if (messagePayload.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                return dbData;
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(messagePayload, 0, iv, 0, iv.length);

            int cipherTextLength = messagePayload.length - iv.length;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(messagePayload, iv.length, cipherText, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            SecretKeySpec keySpec = new SecretKeySpec(getEffectiveKeyBytes(), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Không thể giải mã trường dữ liệu (có thể là dữ liệu cũ chưa mã hóa): {}", e.getMessage());
            return dbData;
        }
    }

    private byte[] getEffectiveKeyBytes() {
        if (staticKeyBytes != null && staticKeyBytes.length == 32) {
            return staticKeyBytes;
        }
        if (!"PRODUCTION".equalsIgnoreCase(environment != null ? environment.trim() : "")) {
            staticKeyBytes = getOrCreatePersistentLocalKey();
            return staticKeyBytes;
        }
        throw new IllegalStateException("FINVAULT_AES_KEY environment variable is REQUIRED. Application cannot start without a persistent encryption key.");
    }

    private synchronized byte[] getOrCreatePersistentLocalKey() {
        try {
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            File keyFile = new File(dataDir, ".finvault_aes.key");
            if (keyFile.exists() && keyFile.length() > 0) {
                String hex = Files.readString(keyFile.toPath(), StandardCharsets.UTF_8).trim();
                byte[] key = hexStringToByteArray(hex);
                if (key.length == 32) {
                    return key;
                }
            }
            // Sinh khóa AES-256 ngẫu nhiên chuẩn mã hóa an toàn
            byte[] newKey = new byte[32];
            secureRandom.nextBytes(newKey);
            String hex = byteArrayToHexString(newKey);
            Files.writeString(keyFile.toPath(), hex, StandardCharsets.UTF_8);
            log.info("AesEncryptionConverter: Đã tạo và lưu khóa AES-256 phát triển cục bộ tại: {}", keyFile.getAbsolutePath());
            return newKey;
        } catch (Exception e) {
            log.error("Không thể khởi tạo khóa cục bộ: {}", e.getMessage(), e);
            throw new IllegalStateException("FINVAULT_AES_KEY environment variable is REQUIRED. Application cannot start without a persistent encryption key.");
        }
    }

    private static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            s = "0" + s;
            len = s.length();
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
