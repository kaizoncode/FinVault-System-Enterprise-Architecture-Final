package com.finvault;

import com.finvault.module1_security_rbac.dto.RegisterRequest;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.service.CustomUserDetailsService;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.controller.MainDashboardController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private MainDashboardController mainDashboardController;

    @Autowired
    private com.finvault.module2_wallet_ledger.service.ReconciliationService reconciliationService;

    @Autowired
    private com.finvault.module2_wallet_ledger.service.TransactionService transactionService;

    @Autowired
    private com.finvault.module4_analytics_reporting.service.AnalyticsService analyticsService;

    @Autowired
    private com.finvault.module5_admin_audit.service.SupportTicketRecoveryService recoveryService;

    @Test
    @DisplayName("1. Kiểm tra tài khoản seeded có thể nạp thông tin và đăng nhập chuẩn xác")
    void testSeededUsersLogin() {
        String[] testUsers = {"admin", "auditor", "family_head", "user1", "user_2fa"};
        for (String username : testUsers) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            assertNotNull(userDetails, "UserDetails không được null đối với user: " + username);
            assertEquals(username, userDetails.getUsername());

            Authentication authRequest = new UsernamePasswordAuthenticationToken(username, "Password@123");
            Authentication authResult = authenticationProvider.authenticate(authRequest);
            assertNotNull(authResult);
            assertTrue(authResult.isAuthenticated(), "Tài khoản " + username + " phải đăng nhập thành công với Password@123");
        }
    }

    @Test
    @DisplayName("2. Kiểm tra đăng ký người dùng mới động không bị lỗi thiếu cột PENDING_TWO_FACTOR_SECRET")
    void testRegisterNewUser() {
        String uniqueUser = "dynamic_user_" + System.currentTimeMillis();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(uniqueUser);
        registerRequest.setEmail(uniqueUser + "@example.com");
        registerRequest.setPassword("TestPass@2026");
        registerRequest.setFullName("Người Dùng Thử Nghiệm Tự Động");
        registerRequest.setPhoneNumber("0988776655");

        UserResponseDto createdUser = userService.registerUser(registerRequest);
        assertNotNull(createdUser, "Người dùng mới tạo không được null");
        assertEquals(uniqueUser, createdUser.getUsername());

        // Kiểm tra đăng nhập với tài khoản vừa tạo
        Authentication authRequest = new UsernamePasswordAuthenticationToken(uniqueUser, "TestPass@2026");
        Authentication authResult = authenticationProvider.authenticate(authRequest);
        assertNotNull(authResult);
        assertTrue(authResult.isAuthenticated());

        // Kiểm tra truy cập Dashboard của người dùng mới tạo
        SecurityContextHolder.getContext().setAuthentication(authResult);
        Model model = new ConcurrentModel();
        String viewName = mainDashboardController.showDashboard(model);
        assertEquals("dashboard", viewName);
        assertNotNull(model.getAttribute("currentUser"));
        assertNotNull(model.getAttribute("wallets"));
        assertNotNull(model.getAttribute("activeBudgets"));
    }

    @Test
    @DisplayName("3. Kiểm tra gia trưởng (family_head) truy cập dashboard không bị lỗi")
    void testFamilyHeadDashboard() {
        Authentication authRequest = new UsernamePasswordAuthenticationToken("family_head", "Password@123");
        Authentication authResult = authenticationProvider.authenticate(authRequest);
        assertNotNull(authResult);
        SecurityContextHolder.getContext().setAuthentication(authResult);

        Model model = new ConcurrentModel();
        String viewName = mainDashboardController.showDashboard(model);
        assertEquals("dashboard", viewName);
        assertNotNull(model.getAttribute("pendingApprovals"));
        assertNotNull(model.getAttribute("familyWallets"));
    }

    @Test
    @DisplayName("4. Kiểm tra full HTTP MockMvc GET /dashboard cho family_head và Thymeleaf template rendering")
    void testFamilyHeadHttpMockMvc() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(user("family_head").roles("FAMILY_HEAD")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @DisplayName("5. Kiểm tra full HTTP MockMvc GET /dashboard cho user1 và Thymeleaf template rendering")
    void testUser1HttpMockMvc() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(user("user1").roles("STANDARD_USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @DisplayName("6. Kiểm tra Super Admin & Auditor Dashboard khi có bản ghi Soft-Delete (Không bị crash 500) và khôi phục dữ liệu")
    void testAdminAndAuditorDashboardWithSoftDeleteRecovery() throws Exception {
        // Tạo 1 giao dịch mới cho user1 rồi xóa mềm để kiểm tra bảng Phục hồi dữ liệu trên trang Admin
        com.finvault.module2_wallet_ledger.dto.TransactionRequest req = new com.finvault.module2_wallet_ledger.dto.TransactionRequest(
                1L, 4L, com.finvault.module2_wallet_ledger.entity.TransactionType.EXPENSE,
                new java.math.BigDecimal("100000.00"), "Giao dịch test xóa mềm & khôi phục"
        );
        com.finvault.module2_wallet_ledger.dto.TransactionResponseDto createdTx = transactionService.createTransaction("user1", req);
        assertNotNull(createdTx.getId());

        // Cố tình cập nhật giao dịch (Edit Transaction) trước khi xóa
        req.setAmount(new java.math.BigDecimal("150000.00"));
        req.setNote("Giao dịch đã chỉnh sửa thành công");
        com.finvault.module2_wallet_ledger.dto.TransactionResponseDto updatedTx = transactionService.updateTransaction(createdTx.getId(), "user1", req);
        assertEquals(0, new java.math.BigDecimal("150000.00").compareTo(updatedTx.getAmount()));

        // Xóa mềm giao dịch
        transactionService.deleteTransaction(createdTx.getId(), "user1");

        // Admin truy cập /dashboard khi softDeletedItems có dữ liệu -> Phải trả về 200 OK (không lỗi 500 TemplateProcessingException)
        String adminHtml = mockMvc.perform(get("/dashboard")
                .with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(adminHtml.contains("Giám Sát Vĩ Mô Hệ Thống"), "Admin phải thấy Widget Giám Sát Vĩ Mô Hệ Thống");

        // Auditor truy cập /dashboard -> Phải trả về 200 OK và che số dư
        String auditorHtml = mockMvc.perform(get("/dashboard")
                .with(user("auditor").roles("SYSTEM_AUDITOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(auditorHtml.contains("***,*** VND"), "Auditor phải thấy số dư được che (Data Masking)");

        // Admin khôi phục lại giao dịch đã xóa mềm và xóa sạch lại để giữ nguyên trạng thái ví
        recoveryService.restoreTransaction(createdTx.getId(), "admin");
        transactionService.deleteTransaction(createdTx.getId(), "user1");
    }

    @Test
    @DisplayName("7. Kiểm tra Đối soát Sổ cái (Reconciliation) 100% khớp trên toàn bộ ví mặc định và cho phép thành viên gia đình đối soát")
    void testReconciliationZeroDiscrepancy() {
        java.util.List<com.finvault.module2_wallet_ledger.dto.ReconciliationReportDto> reports =
                reconciliationService.reconcileAllWallets("admin");
        assertFalse(reports.isEmpty());
        for (com.finvault.module2_wallet_ledger.dto.ReconciliationReportDto r : reports) {
            assertFalse(Boolean.TRUE.equals(r.getIsDiscrepancyDetected()),
                    "Ví " + r.getWalletName() + " không được lệch sổ cái! Độ lệch hiện tại: " + r.getDiscrepancy());
            assertEquals(0, java.math.BigDecimal.ZERO.compareTo(r.getDiscrepancy()));
        }

        // Thành viên gia đình (user1) được quyền đối soát Quỹ chung gia đình (id = 3)
        com.finvault.module2_wallet_ledger.dto.ReconciliationReportDto familyReport =
                reconciliationService.reconcileWallet(3L, "user1");
        assertNotNull(familyReport);
        assertFalse(Boolean.TRUE.equals(familyReport.getIsDiscrepancyDetected()));
    }

    @Test
    @DisplayName("8. Kiểm tra Xuất Báo cáo Excel & PDF trên mọi đường dẫn alias và Biểu đồ BI của Admin")
    void testExportEndpointsAndAdminAnalytics() throws Exception {
        mockMvc.perform(get("/api/reports/export/excel").with(user("user1").roles("STANDARD_USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/transactions/export/excel").with(user("user1").roles("STANDARD_USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/export/pdf").with(user("user1").roles("STANDARD_USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reports/transactions/export/pdf").with(user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk());

        // Kiểm tra Admin nhận được dữ liệu dòng tiền toàn hệ thống thay vì 0 VND
        com.finvault.module4_analytics_reporting.dto.CashFlowTrendDto adminTrend =
                analyticsService.getDailyCashFlowTrend("admin", java.time.LocalDate.now().minusDays(7), java.time.LocalDate.now());
        assertNotNull(adminTrend);
        boolean hasNonZeroSystemFlow = adminTrend.getTotalInflow().compareTo(java.math.BigDecimal.ZERO) > 0
                || adminTrend.getTotalOutflow().compareTo(java.math.BigDecimal.ZERO) > 0;
        assertTrue(hasNonZeroSystemFlow, "Biểu đồ dòng tiền của Admin phải phản ánh giao dịch của toàn hệ thống!");
    }
}
