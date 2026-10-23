package com.finvault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * OpenApiConfig: Cấu hình tài liệu chuẩn hóa REST API theo đặc tả OpenAPI 3.0 (Swagger UI)
 * ==============================================================================
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finVaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinVault Enterprise API")
                        .description("Hệ thống Quản trị Tài chính Cá nhân & Quỹ Gia đình Đa Nền tảng (FinGuard Enterprise)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FinVault Engineering Team - Nhóm 3")
                                .email("contact@finvault.com"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("SessionAuth"))
                .components(new Components()
                        .addSecuritySchemes("SessionAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("Session Cookie xác thực người dùng")));
    }
}
