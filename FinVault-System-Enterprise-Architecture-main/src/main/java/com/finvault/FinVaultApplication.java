package com.finvault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * Main Application Bootstrapper for FinVault Enterprise System
 * (Enterprise Personal & Family Wealth Management System / FinGuard Enterprise)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Microservices Gateway và Service Mesh Discovery trong phiên bản v2.0
 * ==============================================================================
 */
@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FinVaultApplication {

    private static final Logger log = LoggerFactory.getLogger(FinVaultApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(FinVaultApplication.class, args);
        log.info("FinVault Enterprise System has started successfully!");
        log.info("Web Application: http://localhost:8080");
        log.info("H2 Database Console: http://localhost:8080/h2-console (sa / password)");
        log.info("Swagger OpenAPI Documentation: http://localhost:8080/swagger-ui.html");
    }
}
