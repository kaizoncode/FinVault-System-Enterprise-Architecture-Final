package com.finvault.module1_security_rbac.service;

import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.PasswordChangeDto;
import com.finvault.module1_security_rbac.dto.RegisterRequest;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.RoleRepository;
import com.finvault.module1_security_rbac.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * UserServiceImpl: Hiện thực hóa các nghiệp vụ người dùng, mã hóa mật khẩu bằng BCrypt,
 * gán Role mặc định ROLE_STANDARD_USER khi đăng ký.
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tự động gửi Email kích hoạt tài khoản kèm mã Token xác thực 24h
 * ==============================================================================
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserResponseDto registerUser(RegisterRequest request) {
        String cleanUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (userRepository.existsByUsername(cleanUsername)) {
            throw new BusinessException("USERNAME_EXISTS", "Tên đăng nhập '" + cleanUsername + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new BusinessException("EMAIL_EXISTS", "Địa chỉ email '" + cleanEmail + "' đã được sử dụng");
        }

        User user = new User();
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null);
        user.setIsActive(true);
        user.setIsTwoFactorEnabled(false);
        user.setFailedLoginAttempts(0);

        Role defaultRole = roleRepository.findByRoleName(Role.ROLE_STANDARD_USER)
                .orElseGet(() -> roleRepository.save(new Role(Role.ROLE_STANDARD_USER, "Người dùng chuẩn")));
        user.addRole(defaultRole);

        User saved = userRepository.save(user);
        log.info("Đăng ký thành công tài khoản mới: ID [{}], Username [{}]", saved.getId(), saved.getUsername());

        // Phát sự kiện đăng ký thành công để các module liên quan tự động cấp phát tài nguyên
        eventPublisher.publishEvent(new com.finvault.module1_security_rbac.event.UserRegisteredEvent(this, saved));

        return UserResponseDto.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByUsername(String username) {
        User user = getUserEntityByUsername(username);
        return UserResponseDto.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username, HttpStatus.NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAllByIsDeletedFalse().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getLockedUsers() {
        return userRepository.findCurrentlyLockedUsers().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDto updateUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng có ID: " + userId, HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("USER_DELETED", "Không thể thao tác trên tài khoản đã bị xóa", HttpStatus.BAD_REQUEST);
        }

        // Khi vô hiệu hóa tài khoản (isActive = false):
        if (!isActive) {
            // 1. Chặn Super Admin tự vô hiệu hóa tài khoản của chính mình
            String currentUsername = SecurityUtils.getCurrentUsername();
            if (currentUsername != null && user.getUsername().equalsIgnoreCase(currentUsername)) {
                throw new BusinessException("CANNOT_DEACTIVATE_SELF", "Không thể tự vô hiệu hóa tài khoản của chính mình", HttpStatus.BAD_REQUEST);
            }

            // 2. Chặn vô hiệu hóa nếu đây là Super Admin duy nhất đang hoạt động trong hệ thống
            if (user.hasRole(Role.ROLE_SUPER_ADMIN)) {
                long activeSuperAdmins = userRepository.countActiveUsersByRoleName("ROLE_SUPER_ADMIN");
                if (activeSuperAdmins <= 1) {
                    throw new BusinessException("LAST_SUPER_ADMIN", "Phải có ít nhất 1 Super Admin hoạt động", HttpStatus.BAD_REQUEST);
                }
            }
        }

        user.setIsActive(isActive);
        User updated = userRepository.save(user);
        log.info("Super Admin đã cập nhật trạng thái isActive = {} cho người dùng [{}]", isActive, user.getUsername());
        return UserResponseDto.fromEntity(updated);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng có ID: " + userId, HttpStatus.NOT_FOUND));
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new BusinessException("USER_DELETED", "Không thể thao tác trên tài khoản đã bị xóa", HttpStatus.BAD_REQUEST);
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Super Admin đã mở khóa thành công cho người dùng [{}]", user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(String username, PasswordChangeDto dto) {
        User user = getUserEntityByUsername(username);
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Mật khẩu hiện tại không chính xác");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        log.info("Người dùng [{}] đã đổi mật khẩu thành công.", username);
    }
}
