package com.finvault.config;

import com.finvault.common.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * JpaAuditingConfig: Tự động điền createdBy và updatedBy vào BaseEntity
 * ==============================================================================
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            String username = SecurityUtils.getCurrentUsername();
            return Optional.of(username != null ? username : "SYSTEM");
        };
    }
}
