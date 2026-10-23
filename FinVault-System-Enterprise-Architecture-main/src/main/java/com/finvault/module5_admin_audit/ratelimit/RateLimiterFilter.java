package com.finvault.module5_admin_audit.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ==============================================================================
 * [Owner: Member 5 - Admin Live CMS & Audit Logging Module]
 * RateLimiterFilter: Bộ lọc HTTP chặn các yêu cầu vượt ngưỡng tần suất (HTTP 429 Too Many Requests)
 * Được cấu hình gắn trong Spring Security FilterChain sau UsernamePasswordAuthenticationFilter.
 * ==============================================================================
 */
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Bỏ qua tài nguyên tĩnh và trang lỗi hệ thống
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientKey = resolveClientKey(request);

        if (!rateLimiterService.tryConsume(clientKey)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            String path = request.getRequestURI();

            if (path.startsWith("/api/")) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Quá nhiều yêu cầu. Vui lòng thử lại sau giây lát (Rate limit exceeded)\",\"errorCode\":\"TOO_MANY_REQUESTS\"}"
                );
            } else if ("POST".equalsIgnoreCase(request.getMethod()) && ("/login".equals(path) || "/register".equals(path) || "/auth/2fa".equals(path))) {
                response.sendRedirect(request.getContextPath() + path + "?rateLimited=true");
            } else {
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write(
                        "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>429 Too Many Requests</title></head>"
                                + "<body style='font-family:sans-serif;text-align:center;padding:50px;'>"
                                + "<h2>429 - Quá nhiều yêu cầu</h2>"
                                + "<p>FinVault phát hiện số lượng yêu cầu bất thường từ bạn. Vui lòng đợi trong giây lát.</p>"
                                + "<a href='" + request.getContextPath() + "/login'>Quay lại trang Đăng nhập</a>"
                                + "</body></html>"
                );
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean isAuthPath = "/login".equals(path) || "/register".equals(path) || "/auth/2fa".equals(path) || path.startsWith("/api/admin/bootstrap");
        boolean isAuthEndpoint = isAuthPath && "POST".equalsIgnoreCase(request.getMethod());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return (isAuthEndpoint ? "AUTH:USER:" : "USER:") + auth.getName();
        }

        String clientIp = com.finvault.common.SecurityUtils.getClientIpAddress(request);
        return (isAuthEndpoint ? "AUTH:IP:" : "IP:") + clientIp;
    }
}
