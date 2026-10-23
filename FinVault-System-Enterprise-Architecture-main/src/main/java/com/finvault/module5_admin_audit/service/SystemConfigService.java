package com.finvault.module5_admin_audit.service;

import com.finvault.module5_admin_audit.dto.SystemConfigRequest;
import com.finvault.module5_admin_audit.dto.SystemConfigResponseDto;

import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfigService: Interface quản trị cấu hình động hệ thống (Live Admin CMS)
 * ==============================================================================
 */
public interface SystemConfigService {

    List<SystemConfigResponseDto> getAllConfigs();

    List<SystemConfigResponseDto> getConfigsByGroup(String group);

    SystemConfigResponseDto getConfigByKey(String key);

    String getConfigValue(String key, String defaultValue);

    SystemConfigResponseDto updateConfig(String key, String adminUsername, SystemConfigRequest request);

    SystemConfigResponseDto createConfig(String adminUsername, SystemConfigRequest request);

    void deleteConfig(String key, String adminUsername);
}
