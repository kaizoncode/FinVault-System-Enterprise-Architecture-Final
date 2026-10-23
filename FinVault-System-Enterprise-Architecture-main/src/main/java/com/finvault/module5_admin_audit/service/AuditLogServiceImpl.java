package com.finvault.module5_admin_audit.service;

import com.finvault.common.BusinessException;
import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module1_security_rbac.service.BruteForceProtectionService;
import com.finvault.module5_admin_audit.aop.Auditable;
import com.finvault.module5_admin_audit.dto.AuditLogFilterRequest;
import com.finvault.module5_admin_audit.dto.AuditLogResponseDto;
import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.repository.AuditLogRepository;
import com.finvault.module5_admin_audit.util.DataMaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * AuditLogServiceImpl: Triển khai toàn diện tra cứu nhật ký kiểm toán và quản lý bảo mật
 * ==============================================================================
 */
@Service
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final BruteForceProtectionService bruteForceProtectionService;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               UserRepository userRepository,
                               BruteForceProtectionService bruteForceProtectionService) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.bruteForceProtectionService = bruteForceProtectionService;
    }

    private AuditLogResponseDto toMaskedDtoIfNeeded(AuditLog entity) {
        AuditLogResponseDto dto = AuditLogResponseDto.fromEntity(entity);
        if (dto != null && SecurityUtils.hasRole("ROLE_SYSTEM_AUDITOR") && !SecurityUtils.hasRole("ROLE_SUPER_ADMIN")) {
            dto.setOldValue(DataMaskingUtil.maskSensitiveJsonPayload(dto.getOldValue()));
            dto.setNewValue(DataMaskingUtil.maskSensitiveJsonPayload(dto.getNewValue()));
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toMaskedDtoIfNeeded);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDto> filterAuditLogs(AuditLogFilterRequest filter, Pageable pageable) {
        String actor = (filter.getActorUsername() != null && !filter.getActorUsername().isBlank()) ? filter.getActorUsername().trim() : null;
        String module = (filter.getTargetModule() != null && !filter.getTargetModule().isBlank()) ? filter.getTargetModule().trim() : null;
        String action = (filter.getAction() != null && !filter.getAction().isBlank()) ? filter.getAction().trim() : null;

        return auditLogRepository.filterAuditLogs(actor, module, action, filter.getStartDate(), filter.getEndDate(), pageable)
                .map(this::toMaskedDtoIfNeeded);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc().stream()
                .map(this::toMaskedDtoIfNeeded)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getCurrentlyLockedAccounts() {
        List<User> lockedUsers = userRepository.findCurrentlyLockedUsers();
        return lockedUsers.stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Auditable(action = "MANUAL_UNLOCK_ACCOUNT", module = "SECURITY_IAM", entityName = "app_users")
    public void unlockUserAccount(String targetUsername, String adminUsername) {
        log.info("Super Admin [{}] thực hiện mở khóa thủ công cho tài khoản [{}]", adminUsername, targetUsername);

        User targetUser = userRepository.findByUsernameAndIsDeletedFalse(targetUsername)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + targetUsername));

        bruteForceProtectionService.unlockAccountManually(targetUser.getUsername());
        log.info("Mở khóa thành công tài khoản: [{}]", targetUsername);
    }
}
