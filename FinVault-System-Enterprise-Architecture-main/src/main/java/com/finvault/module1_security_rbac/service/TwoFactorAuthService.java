package com.finvault.module1_security_rbac.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.crypto.TotpManager;
import com.finvault.module1_security_rbac.dto.TwoFactorSetupDto;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * TwoFactorAuthService: Xử lý quy trình thiết lập, bật/tắt và xác thực bảo mật 2 lớp (2FA TOTP)
 * Sử dụng Google Authenticator và mã hóa AES-256 cho khóa bí mật.
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ WebAuthn FIDO2 Biometric Passkey song song với TOTP 2FA (v2.0)
 * ==============================================================================
 */
@Service
public class TwoFactorAuthService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthService.class);

    private final UserRepository userRepository;
    private final TotpManager totpManager;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> pendingScratchCodes = new ConcurrentHashMap<>();

    public TwoFactorAuthService(UserRepository userRepository,
                                TotpManager totpManager,
                                @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.totpManager = totpManager;
        this.passwordEncoder = passwordEncoder;
    }

    private PasswordEncoder getPasswordEncoder() {
        return (passwordEncoder != null) ? passwordEncoder : new BCryptPasswordEncoder();
    }

    /**
     * Khởi tạo quá trình đăng ký 2FA: Sinh mã bí mật mới và URL QR Code
     */
    @Transactional
    public TwoFactorSetupDto initiate2FaSetup(String username) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            log.warn("Người dùng [{}] đã kích hoạt 2FA trước đó, đang khởi tạo secret mới thay thế", username);
        }

        String secretKey = totpManager.generateSecretKey();
        user.setPendingTwoFactorSecret(secretKey);

        String qrCodeUrl = totpManager.getQrCodeUrl(username, secretKey);
        List<String> scratchCodes = totpManager.generateScratchCodes();

        // 1. Hash và lưu danh sách Scratch Codes (dạng BCrypt phân cách bởi ";")
        if (scratchCodes != null && !scratchCodes.isEmpty()) {
            String hashedCodes = scratchCodes.stream()
                    .map(c -> getPasswordEncoder().encode(c.trim()))
                    .collect(Collectors.joining(";"));
            pendingScratchCodes.put(username, hashedCodes);
            if (!Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
                user.setScratchCodes(hashedCodes);
            }
        }

        userRepository.save(user);

        log.info("Khởi tạo thiết lập 2FA thành công cho người dùng [{}]", username);
        return new TwoFactorSetupDto(secretKey, qrCodeUrl, scratchCodes);
    }

    /**
     * Kích hoạt 2FA: Xác thực mã 6 số đầu tiên từ ứng dụng Authenticator
     */
    @Transactional
    public boolean confirmAndEnable2Fa(String username, String code) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (user.getPendingTwoFactorSecret() == null || user.getPendingTwoFactorSecret().isBlank()) {
            throw new BusinessException("TWO_FA_NOT_INITIATED", "Bạn chưa khởi tạo thiết lập 2FA");
        }

        boolean isValid = totpManager.verifyCode(user.getPendingTwoFactorSecret(), code);
        if (!isValid) {
            log.warn("Kích hoạt 2FA thất bại cho user [{}]", username);
            return false;
        }

        user.setTwoFactorSecret(user.getPendingTwoFactorSecret());
        user.setPendingTwoFactorSecret(null);
        String pendingHashedScratch = pendingScratchCodes.remove(username);
        if (pendingHashedScratch != null) {
            user.setScratchCodes(pendingHashedScratch);
        }
        user.setIsTwoFactorEnabled(true);
        userRepository.save(user);
        log.info("Kích hoạt bảo mật 2FA (Google Authenticator) THÀNH CÔNG cho người dùng [{}]", username);
        return true;
    }

    /**
     * Hủy kích hoạt 2FA: Yêu cầu xác thực mã TOTP hoặc Scratch Code trước khi tắt
     */
    @Transactional
    public boolean disable2Fa(String username, String code) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            return true;
        }

        boolean isValid = verifyTotpCode(username, code);
        if (!isValid) {
            log.warn("Hủy 2FA thất bại: Mã TOTP hoặc Scratch Code không hợp lệ cho user [{}]", username);
            return false;
        }

        pendingScratchCodes.remove(username);
        user.setIsTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        user.setPendingTwoFactorSecret(null);
        user.setScratchCodes(null);
        userRepository.save(user);
        log.info("Đã tắt bảo mật 2FA thành công cho người dùng [{}]", username);
        return true;
    }

    /**
     * Xác thực mã TOTP hoặc Scratch Code dự phòng trong quá trình đăng nhập
     */
    @Transactional
    public boolean verifyTotpCode(String username, String code) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            return true; // Không bật 2FA thì luôn pass
        }

        if (user.getTwoFactorSecret() == null) {
            return false;
        }

        // 1. Thử xác thực mã TOTP 6 số
        boolean isValidTotp = totpManager.verifyCode(user.getTwoFactorSecret(), code);
        if (isValidTotp) {
            return true;
        }

        // 2. Nếu TOTP fail, kiểm tra xem input có khớp với Scratch Code (mã khôi phục) nào không
        if (code != null && !code.isBlank() && user.getScratchCodes() != null && !user.getScratchCodes().isBlank()) {
            String trimmedInput = code.trim();
            String[] hashedList = user.getScratchCodes().split(";");
            for (int i = 0; i < hashedList.length; i++) {
                String hashed = hashedList[i].trim();
                if (!hashed.isEmpty() && getPasswordEncoder().matches(trimmedInput, hashed)) {
                    // Match! Xóa scratch code này khỏi danh sách (single-use)
                    List<String> remaining = new ArrayList<>(Arrays.asList(hashedList));
                    remaining.remove(i);
                    user.setScratchCodes(String.join(";", remaining));
                    userRepository.save(user);
                    log.info("Xác thực 2FA thành công bằng Scratch Code dự phòng cho user [{}]. Mã đã bị vô hiệu hóa sau sử dụng.", username);
                    return true;
                }
            }
        }

        return false;
    }
}
