package com.finvault.module5_admin_audit.service;

import com.finvault.common.BusinessException;
import com.finvault.module5_admin_audit.aop.Auditable;
import com.finvault.module5_admin_audit.dto.SystemConfigRequest;
import com.finvault.module5_admin_audit.dto.SystemConfigResponseDto;
import com.finvault.module5_admin_audit.entity.SystemConfig;
import com.finvault.module5_admin_audit.repository.SystemConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfigServiceImpl: Triển khai toàn diện quản trị cấu hình hệ thống thời gian thực
 *
 * Tính năng chính:
 * - Thay đổi cấu hình tức thì (Hot-Reload) mà không cần restart server
 * - Tự động ghi vết kiểm toán (AOP @Auditable) khi Admin thay đổi bất kỳ tham số nào
 * - Bảo vệ các tham số cấu hình cố định (is_editable = false)
 * ==============================================================================
 */
@Service
@Transactional
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    private final SystemConfigRepository configRepository;

    public SystemConfigServiceImpl(SystemConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponseDto> getAllConfigs() {
        return configRepository.findAllByOrderByConfigGroupAscConfigKeyAsc().stream()
                .map(SystemConfigResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigResponseDto> getConfigsByGroup(String group) {
        return configRepository.findAllByConfigGroupOrderByConfigKey(group).stream()
                .map(SystemConfigResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SystemConfigResponseDto getConfigByKey(String key) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("CONFIG_NOT_FOUND", "Không tìm thấy cấu hình với khóa: " + key));
        return SystemConfigResponseDto.fromEntity(config);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfigValue(String key, String defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    @Auditable(action = "UPDATE_SYSTEM_CONFIG", module = "SYSTEM_CONFIG", entityName = "system_configs")
    public SystemConfigResponseDto updateConfig(String key, String adminUsername, SystemConfigRequest request) {
        log.info("Admin [{}] cập nhật cấu hình [{}] giá trị mới: [{}]", adminUsername, key, request.getConfigValue());

        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("CONFIG_NOT_FOUND", "Không tìm thấy cấu hình với khóa: " + key));

        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new BusinessException("CONFIG_READONLY", "Tham số cấu hình này được bảo vệ cố định bởi hệ thống và không thể sửa đổi");
        }

        config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            config.setDescription(request.getDescription());
        }
        config.setUpdatedBy(adminUsername);
        config.setUpdatedAt(LocalDateTime.now());

        SystemConfig updated = configRepository.save(config);
        log.info("Cập nhật thành công cấu hình [{}]", key);
        return SystemConfigResponseDto.fromEntity(updated);
    }

    @Override
    @Auditable(action = "CREATE_SYSTEM_CONFIG", module = "SYSTEM_CONFIG", entityName = "system_configs")
    public SystemConfigResponseDto createConfig(String adminUsername, SystemConfigRequest request) {
        if (configRepository.existsByConfigKey(request.getConfigKey())) {
            throw new BusinessException("CONFIG_ALREADY_EXISTS", "Khóa cấu hình đã tồn tại: " + request.getConfigKey());
        }

        SystemConfig config = new SystemConfig(
                request.getConfigKey(),
                request.getConfigValue(),
                "CUSTOM",
                request.getDescription(),
                true,
                adminUsername
        );

        SystemConfig saved = configRepository.save(config);
        log.info("Admin [{}] tạo mới tham số cấu hình: [{}]", adminUsername, saved.getConfigKey());
        return SystemConfigResponseDto.fromEntity(saved);
    }

    @Override
    @Auditable(action = "DELETE_SYSTEM_CONFIG", module = "SYSTEM_CONFIG", entityName = "system_configs")
    public void deleteConfig(String key, String adminUsername) {
        SystemConfig config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("CONFIG_NOT_FOUND", "Không tìm thấy cấu hình với khóa: " + key));

        if (Boolean.FALSE.equals(config.getIsEditable())) {
            throw new BusinessException("CONFIG_READONLY", "Không thể xóa tham số cấu hình mặc định của hệ thống");
        }

        configRepository.delete(config);
        log.info("Admin [{}] đã xóa tham số cấu hình: [{}]", adminUsername, key);
    }
}
