package com.finvault.module5_admin_audit.repository;

import com.finvault.module5_admin_audit.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * SystemConfigRepository: Tầng truy xuất cấu hình động toàn hệ thống
 * ==============================================================================
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    List<SystemConfig> findAllByConfigGroupOrderByConfigKey(String configGroup);

    List<SystemConfig> findAllByOrderByConfigGroupAscConfigKeyAsc();

    boolean existsByConfigKey(String configKey);
}
