package com.finvault.config;

import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.service.BruteForceProtectionService;
import com.finvault.module1_security_rbac.service.CustomUserDetailsService;
import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.repository.AuditLogRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.finvault.module5_admin_audit.ratelimit.RateLimiterFilter;
import com.finvault.module5_admin_audit.ratelimit.RateLimiterService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * SecurityConfig: Cấu hình Spring Security 6 FilterChain, RBAC 4 Roles,
 * Tích hợp CustomUserDetailsService, Brute-force Login Protection, 2FA Redirection,
 * Session-based Authentication, Form Login, Audit Log đăng nhập/đăng xuất và Exception Handling chuẩn.
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp PasskeyAuthenticationProvider cho WebAuthn Passwordless Login (v2.0)
 * ==============================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final AuditLogRepository auditLogRepository;
    private final RateLimiterService rateLimiterService;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            BruteForceProtectionService bruteForceProtectionService,
            AuditLogRepository auditLogRepository,
            RateLimiterService rateLimiterService) {
        this.userDetailsService = userDetailsService;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.auditLogRepository = auditLogRepository;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Quản lý phiên làm việc đa thiết bị và kích hoạt lắng nghe sự kiện hủy session
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            String clientIp = SecurityUtils.getClientIpAddress(request);
            String userAgent = SecurityUtils.getClientUserAgent(request);
            log.warn("Đăng nhập thất bại đối với tài khoản [{}] từ IP [{}]: {}", username, clientIp, exception.getMessage());

            // Ghi vết kiểm toán đăng nhập thất bại vào Database Audit Trail
            try {
                AuditLog failLog = new AuditLog(
                        (username != null && !username.isBlank()) ? username.trim() : "ANONYMOUS",
                        "ANONYMOUS",
                        "LOGIN_FAILED",
                        "IAM_SECURITY",
                        "app_users",
                        null,
                        null,
                        "Đăng nhập thất bại từ IP: " + clientIp,
                        clientIp,
                        userAgent,
                        "FAILED",
                        exception != null ? exception.getMessage() : "Bad credentials"
                );
                auditLogRepository.save(failLog);
            } catch (Exception ex) {
                log.error("Không thể ghi vết kiểm toán đăng nhập thất bại: {}", ex.getMessage());
            }

            if (username != null && !username.isBlank()) {
                bruteForceProtectionService.recordFailedLogin(username.trim());
                if (bruteForceProtectionService.isAccountLocked(username.trim())) {
                    long remaining = bruteForceProtectionService.getRemainingLockMinutes(username.trim());
                    response.sendRedirect(request.getContextPath() + "/login?locked=true&remainingMinutes=" + remaining);
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + "/login?error=true");
        };
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            String username = authentication.getName();
            String clientIp = SecurityUtils.getClientIpAddress(request);
            String userAgent = SecurityUtils.getClientUserAgent(request);
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));

            Object principal = authentication.getPrincipal();
            boolean requires2Fa = (principal instanceof User user && Boolean.TRUE.equals(user.getIsTwoFactorEnabled()));

            // Ghi vết kiểm toán đăng nhập thành công vào Database Audit Trail
            try {
                AuditLog successLog = new AuditLog(
                        username,
                        role.isEmpty() ? "ROLE_STANDARD_USER" : role,
                        requires2Fa ? "LOGIN_2FA_CHALLENGE" : "LOGIN_SUCCESS",
                        "IAM_SECURITY",
                        "app_users",
                        null,
                        null,
                        (requires2Fa ? "Xác thực mật khẩu bước 1 thành công, chuyển sang thử thách 2FA từ IP: " : "Đăng nhập thành công từ IP: ") + clientIp,
                        clientIp,
                        userAgent,
                        "SUCCESS",
                        null
                );
                auditLogRepository.save(successLog);
            } catch (Exception ex) {
                log.error("Không thể ghi vết kiểm toán đăng nhập thành công: {}", ex.getMessage());
            }

            if (requires2Fa) {
                // Nếu bật 2FA -> Đặt username vào Session và xóa Authentication tạm thời khỏi SecurityContext để chống 2FA bypass
                request.getSession().setAttribute("PRE_AUTH_USER", username);
                SecurityContextHolder.clearContext();
                request.getSession().removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                log.info("Tài khoản [{}] yêu cầu xác thực bước 2 (2FA TOTP), đã cô lập phiên và chuyển hướng tới /auth/2fa", username);
                response.sendRedirect(request.getContextPath() + "/auth/2fa");
                return;
            }

            // Tài khoản không bật 2FA -> Ghi nhận đăng nhập thành công và cho phép vào Dashboard
            log.info("Người dùng [{}] đã đăng nhập thông tin cơ bản thành công từ IP [{}].", username, clientIp);
            bruteForceProtectionService.recordSuccessfulLogin(username);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Gắn RateLimiterFilter ngay trước UsernamePasswordAuthenticationFilter để bảo vệ cả POST /login
        RateLimiterFilter rateLimiterFilter = new RateLimiterFilter(rateLimiterService);
        http.addFilterBefore(rateLimiterFilter, UsernamePasswordAuthenticationFilter.class);

        http
            .authenticationProvider(authenticationProvider())
            // Kích hoạt bảo vệ CSRF chuẩn qua CookieCsrfTokenRepository (miễn trừ H2 Console, WebSocket STOMP và Logout)
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/h2-console/**"),
                    new AntPathRequestMatcher("/ws/**"),
                    new AntPathRequestMatcher("/logout")
                )
            )
            // Cấu hình hiển thị iframe cho H2 Web Console
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            // Phân quyền URL đa cấp độ Dynamic RBAC
            .authorizeHttpRequests(auth -> auth
                // Tài nguyên tĩnh công khai & trang đăng nhập/đăng ký/đăng xuất & bootstrap
                .requestMatchers(
                    "/login", "/register", "/auth/2fa", "/logout", "/css/**", "/js/**", "/images/**",
                    "/webjars/**", "/favicon.ico", "/error", "/api/admin/bootstrap"
                ).permitAll()
                // H2 Console chỉ dành riêng cho SUPER_ADMIN
                .requestMatchers("/h2-console/**").hasRole("SUPER_ADMIN")
                // Swagger Documentation yêu cầu xác thực
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                // STOMP WebSocket Endpoint
                .requestMatchers("/ws/**").permitAll()
                // Phân quyền theo 4 Roles
                .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/audit/**").hasAnyRole("SUPER_ADMIN", "SYSTEM_AUDITOR")
                .requestMatchers("/api/family/**").hasAnyRole("SUPER_ADMIN", "FAMILY_HEAD")
                // Mọi request còn lại yêu cầu đăng nhập
                .anyRequest().authenticated()
            )
            // Cấu hình Form Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            // Cấu hình Đăng xuất an toàn: Hỗ trợ linh hoạt cả GET và POST
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        String username = authentication.getName();
                        String clientIp = SecurityUtils.getClientIpAddress(request);
                        String userAgent = SecurityUtils.getClientUserAgent(request);
                        String role = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.joining(","));
                        try {
                            AuditLog logoutLog = new AuditLog(
                                    username,
                                    role.isEmpty() ? "ROLE_STANDARD_USER" : role,
                                    "LOGOUT_SUCCESS",
                                    "IAM_SECURITY",
                                    "app_users",
                                    null,
                                    null,
                                    "Đăng xuất an toàn từ IP: " + clientIp,
                                    clientIp,
                                    userAgent,
                                    "SUCCESS",
                                    null
                            );
                            auditLogRepository.save(logoutLog);
                        } catch (Exception ex) {
                            log.error("Không thể ghi vết kiểm toán đăng xuất: {}", ex.getMessage());
                        }
                        log.info("Người dùng [{}] đã đăng xuất an toàn khỏi hệ thống từ IP [{}].", username, clientIp);
                    }
                    Cookie jsessionCookie = new Cookie("JSESSIONID", "");
                    jsessionCookie.setPath("/");
                    jsessionCookie.setMaxAge(0);
                    jsessionCookie.setHttpOnly(true);
                    response.addCookie(jsessionCookie);

                    Cookie xsrfCookie = new Cookie("XSRF-TOKEN", "");
                    xsrfCookie.setPath("/");
                    xsrfCookie.setMaxAge(0);
                    response.addCookie(xsrfCookie);

                    response.sendRedirect(request.getContextPath() + "/login?logout=true");
                })
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            // Quản lý phiên làm việc Session
            .sessionManagement(session -> session
                .maximumSessions(3)
                .expiredUrl("/login?expired=true")
            )
            // Xử lý ngoại lệ tầng Security Filter Chain
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Truy cập bị từ chối: bạn không đủ thẩm quyền\",\"errorCode\":\"FORBIDDEN\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/error?denied=true");
                    }
                })
                .authenticationEntryPoint((request, response, authException) -> {
                    String uri = request.getRequestURI();
                    if (uri.startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"success\":false,\"message\":\"Yêu cầu xác thực tài khoản\",\"errorCode\":\"UNAUTHORIZED\"}");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
            );

        return http.build();
    }
}
