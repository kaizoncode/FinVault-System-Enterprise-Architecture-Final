package com.finvault.module1_security_rbac.controller;

import com.finvault.common.SecurityUtils;
import com.finvault.module1_security_rbac.dto.RegisterRequest;
import com.finvault.module1_security_rbac.service.BruteForceProtectionService;
import com.finvault.module1_security_rbac.service.CustomUserDetailsService;
import com.finvault.module1_security_rbac.service.TwoFactorAuthService;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module5_admin_audit.entity.AuditLog;
import com.finvault.module5_admin_audit.repository.AuditLogRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 1 - Security & IAM Module]
 * AuthController: Điều khiển giao diện Đăng nhập, Đăng ký và Xác thực 2 bước (2FA)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Hỗ trợ WebAuthn FIDO2 Biometric Login UI (v2.0)
 * ==============================================================================
 */
@Controller
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuditLogRepository auditLogRepository;

    public AuthController(
            UserService userService,
            TwoFactorAuthService twoFactorAuthService,
            BruteForceProtectionService bruteForceProtectionService,
            CustomUserDetailsService customUserDetailsService) {
        this(userService, twoFactorAuthService, bruteForceProtectionService, customUserDetailsService, null);
    }

    @Autowired
    public AuthController(
            UserService userService,
            TwoFactorAuthService twoFactorAuthService,
            BruteForceProtectionService bruteForceProtectionService,
            CustomUserDetailsService customUserDetailsService,
            AuditLogRepository auditLogRepository) {
        this.userService = userService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.customUserDetailsService = customUserDetailsService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "locked", required = false) String locked,
            @RequestParam(value = "remainingMinutes", required = false) String remainingMinutes,
            @RequestParam(value = "switchUser", required = false) String switchUser,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model,
            Authentication authentication) {

        // Nếu request đăng xuất hoặc đổi tài khoản, giải phóng session và security context ngay lập tức
        if (logout != null || switchUser != null) {
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }

            // Xóa triệt để các Cookie xác thực của phiên làm việc cũ
            Cookie jsessionCookie = new Cookie("JSESSIONID", "");
            jsessionCookie.setPath("/");
            jsessionCookie.setMaxAge(0);
            jsessionCookie.setHttpOnly(true);
            response.addCookie(jsessionCookie);

            Cookie xsrfCookie = new Cookie("XSRF-TOKEN", "");
            xsrfCookie.setPath("/");
            xsrfCookie.setMaxAge(0);
            response.addCookie(xsrfCookie);

            authentication = null;
        }

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }

        if (switchUser != null) {
            model.addAttribute("prefillUsername", switchUser);
            model.addAttribute("successMessage", "Vui lòng nhập mật khẩu để đăng nhập tài khoản [" + switchUser + "].");
        } else if (locked != null) {
            model.addAttribute("errorMessage", "Tài khoản của bạn tạm thời bị khóa " +
                    (remainingMinutes != null ? remainingMinutes : "15") + " phút do nhập sai mật khẩu quá 5 lần.");
        } else if (error != null) {
            model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác.");
        }

        if (logout != null && switchUser == null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất an toàn khỏi hệ thống.");
        }

        return "login";
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logoutPage(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Xóa triệt để Cookie xác thực để đảm bảo đăng xuất hoàn toàn
        Cookie jsessionCookie = new Cookie("JSESSIONID", "");
        jsessionCookie.setPath("/");
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setHttpOnly(true);
        response.addCookie(jsessionCookie);

        Cookie xsrfCookie = new Cookie("XSRF-TOKEN", "");
        xsrfCookie.setPath("/");
        xsrfCookie.setMaxAge(0);
        response.addCookie(xsrfCookie);

        return "redirect:/login?logout=true";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(request);
            return "redirect:/login?registered=true";
        } catch (Exception ex) {
            log.warn("Đăng ký tài khoản thất bại: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }

    @GetMapping("/auth/2fa")
    public String show2FaVerificationPage(HttpSession session, Model model) {
        String preAuthUser = (String) session.getAttribute("PRE_AUTH_USER");
        if (preAuthUser == null) {
            return "redirect:/login";
        }
        if (bruteForceProtectionService.isAccountLocked(preAuthUser)) {
            long remaining = bruteForceProtectionService.getRemainingLockMinutes(preAuthUser);
            session.removeAttribute("PRE_AUTH_USER");
            return "redirect:/login?locked=true&remainingMinutes=" + remaining;
        }
        model.addAttribute("username", preAuthUser);
        return "2fa_verify";
    }

    @PostMapping("/auth/2fa")
    public String verify2FaLogin(
            @RequestParam("code") String code,
            HttpSession session,
            HttpServletRequest request,
            Model model) {

        String preAuthUser = (String) session.getAttribute("PRE_AUTH_USER");
        if (preAuthUser == null) {
            return "redirect:/login";
        }

        // Kiểm tra xem tài khoản có đang bị khóa không trước khi verify
        if (bruteForceProtectionService.isAccountLocked(preAuthUser)) {
            long remaining = bruteForceProtectionService.getRemainingLockMinutes(preAuthUser);
            session.removeAttribute("PRE_AUTH_USER");
            return "redirect:/login?locked=true&remainingMinutes=" + remaining;
        }

        String clientIp = SecurityUtils.getClientIpAddress(request);
        String userAgent = SecurityUtils.getClientUserAgent(request);

        boolean isValid = twoFactorAuthService.verifyTotpCode(preAuthUser, code);
        if (!isValid) {
            // Ghi nhận lần thử thất bại vào hệ thống chống brute-force và Audit Log
            bruteForceProtectionService.recordFailedLogin(preAuthUser);
            if (auditLogRepository != null) {
                try {
                    AuditLog failLog = new AuditLog(
                            preAuthUser,
                            "PRE_2FA_AUTH",
                            "LOGIN_2FA_FAILED",
                            "IAM_SECURITY",
                            "app_users",
                            null,
                            null,
                            "Xác thực 2FA thất bại từ IP: " + clientIp,
                            clientIp,
                            userAgent,
                            "FAILED",
                            "Invalid TOTP or Scratch Code"
                    );
                    auditLogRepository.save(failLog);
                } catch (Exception ex) {
                    log.error("Không thể ghi vết kiểm toán 2FA thất bại: {}", ex.getMessage());
                }
            }
            if (bruteForceProtectionService.isAccountLocked(preAuthUser)) {
                long remaining = bruteForceProtectionService.getRemainingLockMinutes(preAuthUser);
                session.removeAttribute("PRE_AUTH_USER");
                return "redirect:/login?locked=true&remainingMinutes=" + remaining;
            }
            model.addAttribute("username", preAuthUser);
            model.addAttribute("errorMessage", "Mã xác thực 2FA không chính xác hoặc đã hết hạn.");
            return "2fa_verify";
        }

        // Chống Session Fixation sau khi nâng quyền qua 2FA
        try {
            request.changeSessionId();
        } catch (Exception ignored) {
        }

        // Nạp UserDetails và tái thiết lập SecurityContext chính thức sau khi vượt qua 2FA
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(preAuthUser);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        if (auditLogRepository != null) {
            try {
                String roles = userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","));
                AuditLog successLog = new AuditLog(
                        preAuthUser,
                        roles.isEmpty() ? "ROLE_STANDARD_USER" : roles,
                        "LOGIN_2FA_SUCCESS",
                        "IAM_SECURITY",
                        "app_users",
                        null,
                        null,
                        "Xác thực 2FA thành công từ IP: " + clientIp,
                        clientIp,
                        userAgent,
                        "SUCCESS",
                        null
                );
                auditLogRepository.save(successLog);
            } catch (Exception ex) {
                log.error("Không thể ghi vết kiểm toán 2FA thành công: {}", ex.getMessage());
            }
        }

        bruteForceProtectionService.recordSuccessfulLogin(preAuthUser);
        session.removeAttribute("PRE_AUTH_USER");
        session.setAttribute("TWO_FACTOR_VERIFIED", true);
        log.info("Xác thực 2FA đăng nhập thành công cho người dùng [{}]", preAuthUser);
        return "redirect:/dashboard";
    }
}
