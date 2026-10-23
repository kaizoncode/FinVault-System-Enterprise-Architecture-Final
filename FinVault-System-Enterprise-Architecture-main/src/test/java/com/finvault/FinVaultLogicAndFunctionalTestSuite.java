package com.finvault;

import com.finvault.common.ApiResponse;
import com.finvault.common.BusinessException;
import com.finvault.controller.MainDashboardController;
import com.finvault.module1_security_rbac.dto.RegisterRequest;
import com.finvault.module1_security_rbac.dto.UserResponseDto;
import com.finvault.module1_security_rbac.entity.Role;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module1_security_rbac.service.CustomUserDetailsService;
import com.finvault.module1_security_rbac.service.UserService;
import com.finvault.module2_wallet_ledger.dto.*;
import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module2_wallet_ledger.entity.Wallet;
import com.finvault.module2_wallet_ledger.entity.WalletType;
import com.finvault.module2_wallet_ledger.repository.WalletRepository;
import com.finvault.module2_wallet_ledger.service.CategoryService;
import com.finvault.module2_wallet_ledger.service.ReconciliationService;
import com.finvault.module2_wallet_ledger.service.TransactionService;
import com.finvault.module2_wallet_ledger.service.WalletService;
import com.finvault.module3_budget_goals.dto.*;
import com.finvault.module3_budget_goals.entity.BudgetType;
import com.finvault.module3_budget_goals.entity.SavingGoalStatus;
import com.finvault.module3_budget_goals.service.BudgetService;
import com.finvault.module3_budget_goals.service.LoanCalculatorService;
import com.finvault.module3_budget_goals.service.SavingGoalService;
import com.finvault.module4_analytics_reporting.service.ReportExportImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FinVaultLogicAndFunctionalTestSuite {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private SavingGoalService savingGoalService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private LoanCalculatorService loanCalculatorService;

    @Autowired
    private ReportExportImportService reportService;

    @Autowired
    private ReconciliationService reconciliationService;

    // =========================================================================
    // PHẦN 1: TEST ĐĂNG NHẬP, ĐĂNG KÝ VÀ QUYỀN HẠN
    // =========================================================================

    @Test
    @DisplayName("1.1 Đăng ký tài khoản mới qua HTTP POST /register và kiểm tra hệ sinh thái ban đầu")
    void testRegistrationHttpAndEcosystem() throws Exception {
        String testUser = "register_test_" + System.currentTimeMillis();
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", testUser)
                        .param("email", testUser + "@example.com")
                        .param("password", "SecurePass@123")
                        .param("fullName", "Nguyễn Văn Đăng Ký")
                        .param("phoneNumber", "0909123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        // Xác nhận người dùng đã tồn tại
        User user = userRepository.findByUsernameAndIsDeletedFalse(testUser).orElse(null);
        assertNotNull(user, "User phải được lưu vào cơ sở dữ liệu");
        assertTrue(user.hasRole(Role.ROLE_STANDARD_USER));

        // Kiểm tra Event Listener đã tự động tạo ví cá nhân và ngân sách ban đầu
        List<WalletResponseDto> wallets = walletService.getUserWallets(testUser);
        assertFalse(wallets.isEmpty(), "Người dùng mới phải có ít nhất 1 ví tiền mặt ban đầu");
        assertEquals(new BigDecimal("10000000.00"), wallets.get(0).getBalance(), "Ví ban đầu phải có 10.000.000 VND");

        List<BudgetResponseDto> budgets = budgetService.getUserBudgets(testUser);
        assertFalse(budgets.isEmpty(), "Người dùng mới phải có ít nhất 1 ngân sách thông minh ban đầu");
    }

    @Test
    @DisplayName("1.2 Đăng ký trùng username hoặc email phải báo lỗi rõ ràng")
    void testDuplicateRegistration() {
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("user1"); // đã tồn tại trong data.sql
        req1.setEmail("different@email.com");
        req1.setPassword("Password@123");
        req1.setFullName("User trùng");

        BusinessException ex1 = assertThrows(BusinessException.class, () -> userService.registerUser(req1));
        assertEquals("USERNAME_EXISTS", ex1.getErrorCode());

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("brand_new_user");
        req2.setEmail("user1@finvault.com"); // email đã tồn tại trong data.sql
        req2.setPassword("Password@123");
        req2.setFullName("User trùng email");

        BusinessException ex2 = assertThrows(BusinessException.class, () -> userService.registerUser(req2));
        assertEquals("EMAIL_EXISTS", ex2.getErrorCode());
    }

    @Test
    @DisplayName("1.3 Đăng ký nhiều tài khoản liên tiếp và đồng bộ Sequence không bị xung đột Primary Key key: 1000")
    void testSequentialRegistrationAndSequenceResilience() throws Exception {
        for (int i = 1; i <= 3; i++) {
            String username = "seq_user_" + i + "_" + System.currentTimeMillis();
            mockMvc.perform(post("/register")
                            .with(csrf())
                            .param("username", username)
                            .param("email", username + "@example.com")
                            .param("password", "Pass@123456")
                            .param("fullName", "User Liên Tiếp " + i)
                            .param("phoneNumber", "091122334" + i))
                    .andExpect(status().is3xxRedirection());

            List<WalletResponseDto> wallets = walletService.getUserWallets(username);
            assertFalse(wallets.isEmpty(), "Mỗi người dùng liên tiếp phải tạo được ví ban đầu");
            assertTrue(wallets.get(0).getId() >= 1000L, "ID ví phải >= 1000");
        }
    }

    // =========================================================================
    // PHẦN 2: TEST QUẢN LÝ VÍ & LOGIC SỐ DƯ
    // =========================================================================

    @Test
    @DisplayName("2.1 Tạo ví mới (CASH, BANK, CREDIT_CARD) và kiểm tra validation thực tế")
    void testWalletCreationAndValidation() {
        // Tạo ví CASH thành công
        WalletRequest cashReq = new WalletRequest();
        cashReq.setWalletName("Ví Tiền Mặt Test");
        cashReq.setWalletType(WalletType.CASH);
        cashReq.setInitialBalance(new BigDecimal("2000000.00"));
        cashReq.setCurrency("VND");

        WalletResponseDto createdCash = walletService.createWallet("user1", cashReq);
        assertNotNull(createdCash.getId());
        assertEquals("Ví Tiền Mặt Test", createdCash.getWalletName());
        assertEquals(new BigDecimal("2000000.00"), createdCash.getBalance());

        // Tạo ví BANK nhưng thiếu số tài khoản -> phải ném ngoại lệ
        WalletRequest bankReqMissing = new WalletRequest();
        bankReqMissing.setWalletName("Ví VCB Thiếu STK");
        bankReqMissing.setWalletType(WalletType.BANK);
        bankReqMissing.setInitialBalance(BigDecimal.ZERO);
        bankReqMissing.setCurrency("VND");
        bankReqMissing.setBankName("Vietcombank");
        // bankAccountNumber = null

        BusinessException exBank = assertThrows(BusinessException.class, () -> walletService.createWallet("user1", bankReqMissing));
        assertEquals("BANK_ACCOUNT_REQUIRED", exBank.getErrorCode());

        // Số dư ban đầu âm -> phải bị từ chối
        WalletRequest negativeReq = new WalletRequest();
        negativeReq.setWalletName("Ví Âm Tiền");
        negativeReq.setWalletType(WalletType.CASH);
        negativeReq.setInitialBalance(new BigDecimal("-50000.00"));

        BusinessException exNeg = assertThrows(BusinessException.class, () -> walletService.createWallet("user1", negativeReq));
        assertEquals("INVALID_AMOUNT", exNeg.getErrorCode());
    }

    // =========================================================================
    // PHẦN 3: TEST GIAO DỊCH, TRỪ TIỀN VÍ & ĐẢO NGƯỢC
    // =========================================================================

    @Test
    @DisplayName("3.1 Giao dịch Thu/Chi: Cập nhật số dư ví, chống chi quá số dư (Overdraft) và hoàn tiền khi xóa")
    void testTransactionBalanceAndOverdraft() {
        // Tạo một ví riêng cho test này
        WalletRequest wReq = new WalletRequest();
        wReq.setWalletName("Ví Test Thu Chi");
        wReq.setWalletType(WalletType.CASH);
        wReq.setInitialBalance(new BigDecimal("1000000.00"));
        wReq.setCurrency("VND");
        WalletResponseDto w = walletService.createWallet("user1", wReq);
        Long walletId = w.getId();

        // 1. Thêm INCOME 500,000 -> Số dư phải là 1,500,000
        TransactionRequest incomeReq = new TransactionRequest();
        incomeReq.setWalletId(walletId);
        incomeReq.setTransactionType(TransactionType.INCOME);
        incomeReq.setAmount(new BigDecimal("500000.00"));
        incomeReq.setNote("Thu nhập thử nghiệm");
        incomeReq.setTransactionDate(LocalDateTime.now());

        TransactionResponseDto incomeTx = transactionService.createTransaction("user1", incomeReq);
        assertEquals(ApprovalStatus.APPROVED, incomeTx.getApprovalStatus());

        Wallet updatedW1 = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("1500000.00"), updatedW1.getBalance(), "Số dư sau thu nhập phải là 1,500,000 VND");

        // 2. Thêm EXPENSE 300,000 -> Số dư phải là 1,200,000
        TransactionRequest expenseReq = new TransactionRequest();
        expenseReq.setWalletId(walletId);
        expenseReq.setTransactionType(TransactionType.EXPENSE);
        expenseReq.setAmount(new BigDecimal("300000.00"));
        expenseReq.setNote("Chi tiêu thử nghiệm");
        expenseReq.setTransactionDate(LocalDateTime.now());

        TransactionResponseDto expenseTx = transactionService.createTransaction("user1", expenseReq);
        Wallet updatedW2 = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("1200000.00"), updatedW2.getBalance(), "Số dư sau chi tiêu phải là 1,200,000 VND");

        // 3. Chi tiêu vượt quá số dư (2,000,000 > 1,200,000) đối với ví CASH -> phải ném lỗi INSUFFICIENT_FUNDS
        TransactionRequest overReq = new TransactionRequest();
        overReq.setWalletId(walletId);
        overReq.setTransactionType(TransactionType.EXPENSE);
        overReq.setAmount(new BigDecimal("2000000.00"));
        overReq.setNote("Chi tiêu quá tay");

        BusinessException exOver = assertThrows(BusinessException.class, () -> transactionService.createTransaction("user1", overReq));
        assertEquals("INSUFFICIENT_FUNDS", exOver.getErrorCode());

        // 4. Xóa (Soft Delete) giao dịch chi tiêu -> Phải hoàn trả 300,000 VND về ví thành 1,500,000 VND
        transactionService.deleteTransaction(expenseTx.getId(), "user1");
        Wallet updatedW3 = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("1500000.00"), updatedW3.getBalance(), "Sau khi xóa chi tiêu, tiền phải được hoàn trả vào ví");

        // 5. Xóa (Soft Delete) giao dịch thu nhập -> Phải trừ lại 500,000 VND khỏi ví thành 1,000,000 VND
        transactionService.deleteTransaction(incomeTx.getId(), "user1");
        Wallet updatedW4 = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("1000000.00"), updatedW4.getBalance(), "Sau khi xóa thu nhập, số dư ví phải quay về ban đầu");
    }

    @Test
    @DisplayName("3.2 Chuyển tiền liên ví (Inter-wallet Transfer): Kiểm tra chống chuyển cùng ví, số dư 2 ví và hoàn tác")
    void testInterWalletTransferLogic() {
        // Tạo 2 ví nguồn và đích
        WalletRequest w1 = new WalletRequest();
        w1.setWalletName("Ví Chuyển Nguồn");
        w1.setWalletType(WalletType.CASH);
        w1.setInitialBalance(new BigDecimal("2000000.00"));
        WalletResponseDto source = walletService.createWallet("user1", w1);

        WalletRequest w2 = new WalletRequest();
        w2.setWalletName("Ví Chuyển Đích");
        w2.setWalletType(WalletType.CASH);
        w2.setInitialBalance(new BigDecimal("500000.00"));
        WalletResponseDto target = walletService.createWallet("user1", w2);

        // Chuyển cùng 1 ví -> phải ném lỗi SAME_WALLET_TRANSFER
        TransferRequest sameReq = new TransferRequest(source.getId(), source.getId(), new BigDecimal("100000.00"), "Chuyển cho chính mình");
        BusinessException exSame = assertThrows(BusinessException.class, () -> transactionService.transferFunds("user1", sameReq));
        assertEquals("SAME_WALLET_TRANSFER", exSame.getErrorCode());

        // Chuyển 1,000,000 VND từ nguồn sang đích
        TransferRequest validTransfer = new TransferRequest(source.getId(), target.getId(), new BigDecimal("1000000.00"), "Chuyển tiền sinh hoạt");
        TransactionResponseDto transferTx = transactionService.transferFunds("user1", validTransfer);
        assertNotNull(transferTx);

        // Kiểm tra số dư 2 ví
        Wallet sWallet = walletRepository.findById(source.getId()).orElseThrow();
        Wallet tWallet = walletRepository.findById(target.getId()).orElseThrow();
        assertEquals(new BigDecimal("1000000.00"), sWallet.getBalance(), "Ví nguồn phải bị trừ 1,000,000 VND");
        assertEquals(new BigDecimal("1500000.00"), tWallet.getBalance(), "Ví đích phải được cộng 1,000,000 VND");

        // Xóa giao dịch chuyển tiền -> Hoàn lại 1,000,000 cho nguồn, thu hồi từ đích
        transactionService.deleteTransaction(transferTx.getId(), "user1");
        Wallet sRestored = walletRepository.findById(source.getId()).orElseThrow();
        Wallet tRestored = walletRepository.findById(target.getId()).orElseThrow();
        assertEquals(new BigDecimal("2000000.00"), sRestored.getBalance(), "Ví nguồn phải được hoàn trả đủ");
        assertEquals(new BigDecimal("500000.00"), tRestored.getBalance(), "Ví đích phải bị thu hồi khoản đã chuyển");
    }

    // =========================================================================
    // PHẦN 4: TEST DUYỆT CHI GIA TRƯỞNG (FAMILY HEAD APPROVAL WORKFLOW)
    // =========================================================================

    @Test
    @DisplayName("4.1 Khoản chi Quỹ gia đình >= 5,000,000 VND chuyển PENDING và chỉ trừ tiền khi Gia trưởng duyệt")
    void testFamilyFundApprovalWorkflow() {
        // Quỹ gia đình ID 3 (được tạo sẵn trong data.sql) có số dư 185.000.000 VND, user1 là MEMBER
        Wallet familyWalletBefore = walletRepository.findById(3L).orElseThrow();
        BigDecimal balanceBefore = familyWalletBefore.getBalance();

        // 1. user1 chi tiêu 6,000,000 VND từ Quỹ gia đình (vượt ngưỡng 5,000,000 VND)
        TransactionRequest pendingReq = new TransactionRequest();
        pendingReq.setWalletId(3L);
        pendingReq.setTransactionType(TransactionType.EXPENSE);
        pendingReq.setAmount(new BigDecimal("6000000.00"));
        pendingReq.setNote("Mua sắm sofa mới cho phòng khách");
        pendingReq.setTransactionDate(LocalDateTime.now());

        TransactionResponseDto pendingTx = transactionService.createTransaction("user1", pendingReq);
        assertEquals(ApprovalStatus.PENDING, pendingTx.getApprovalStatus(), "Khoản chi >= 5M từ Quỹ gia đình phải PENDING");

        // Số dư ví gia đình CHƯA ĐƯỢC PHÉP TRỪ
        Wallet familyWalletPending = walletRepository.findById(3L).orElseThrow();
        assertEquals(balanceBefore, familyWalletPending.getBalance(), "Số dư ví gia đình không được trừ khi giao dịch còn PENDING");

        // 2. Gia trưởng (family_head) kiểm tra danh sách chờ duyệt
        List<TransactionResponseDto> pendings = transactionService.getPendingFamilyTransactions("family_head");
        assertTrue(pendings.stream().anyMatch(tx -> tx.getId().equals(pendingTx.getId())), "Gia trưởng phải thấy giao dịch cần duyệt");

        // 3. Gia trưởng PHÊ DUYỆT giao dịch
        ApprovalDecisionDto decision = new ApprovalDecisionDto(true, "Duyệt mua sắm hợp lý");
        TransactionResponseDto approvedTx = transactionService.approveOrRejectTransaction(pendingTx.getId(), "family_head", decision);
        assertEquals(ApprovalStatus.APPROVED, approvedTx.getApprovalStatus());

        // Số dư ví gia đình BÂY GIỜ MỚI BỊ TRỪ
        Wallet familyWalletAfter = walletRepository.findById(3L).orElseThrow();
        assertEquals(balanceBefore.subtract(new BigDecimal("6000000.00")), familyWalletAfter.getBalance(), "Số dư ví phải bị trừ sau khi Gia trưởng duyệt");
    }

    // =========================================================================
    // PHẦN 5: TEST NGÂN SÁCH THÔNG MINH 50/30/20 & MỤC TIÊU TIẾT KIỆM
    // =========================================================================

    @Test
    @DisplayName("5.1 Tạo bộ 3 ngân sách 50/30/20 chuẩn xác theo tỷ lệ thu nhập")
    void test503020BudgetGeneration() {
        Budget50_30_20Request req = new Budget50_30_20Request();
        req.setMonthlyIncome(new BigDecimal("20000000.00"));
        req.setStartDate(LocalDate.now());
        req.setEndDate(LocalDate.now().plusMonths(1));

        List<BudgetResponseDto> budgets = budgetService.create50_30_20Budgets("user1", req);
        assertEquals(3, budgets.size(), "Phải tạo đúng 3 ngân sách");

        BudgetResponseDto needs = budgets.stream().filter(b -> com.finvault.module3_budget_goals.entity.AllocationType.NEEDS == b.getAllocationType()).findFirst().orElseThrow();
        BudgetResponseDto wants = budgets.stream().filter(b -> com.finvault.module3_budget_goals.entity.AllocationType.WANTS == b.getAllocationType()).findFirst().orElseThrow();
        BudgetResponseDto savings = budgets.stream().filter(b -> com.finvault.module3_budget_goals.entity.AllocationType.SAVINGS == b.getAllocationType()).findFirst().orElseThrow();

        assertEquals(new BigDecimal("10000000.00"), needs.getAllocatedAmount(), "Needs phải là 50% = 10,000,000 VND");
        assertEquals(new BigDecimal("6000000.00"), wants.getAllocatedAmount(), "Wants phải là 30% = 6,000,000 VND");
        assertEquals(new BigDecimal("4000000.00"), savings.getAllocatedAmount(), "Savings phải là 20% = 4,000,000 VND");
    }

    @Test
    @DisplayName("5.2 Quỹ mục tiêu tiết kiệm: Nạp tiền từ ví, trừ số dư ví, tăng tiền tích lũy và hoàn thành mục tiêu")
    void testSavingGoalDepositAndAchievement() {
        // Tạo ví nguồn có 5,000,000 VND
        WalletRequest w = new WalletRequest();
        w.setWalletName("Ví Nguồn Tiết Kiệm");
        w.setWalletType(WalletType.CASH);
        w.setInitialBalance(new BigDecimal("5000000.00"));
        WalletResponseDto sourceWallet = walletService.createWallet("user1", w);

        // Tạo mục tiêu tiết kiệm 2,000,000 VND
        SavingGoalRequest goalReq = new SavingGoalRequest();
        goalReq.setGoalName("Quỹ mua tai nghe chống ồn");
        goalReq.setTargetAmount(new BigDecimal("2000000.00"));
        goalReq.setInitialAmount(BigDecimal.ZERO);
        goalReq.setTargetDate(LocalDate.now().plusMonths(3));
        goalReq.setNotes("Tập trung làm việc");

        SavingGoalResponseDto goal = savingGoalService.createGoal("user1", goalReq);
        assertNotNull(goal.getId());
        assertEquals(SavingGoalStatus.IN_PROGRESS, goal.getStatus());

        // Nạp 1,000,000 VND từ ví vào quỹ mục tiêu
        SavingGoalDepositRequest depReq1 = new SavingGoalDepositRequest(new BigDecimal("1000000.00"), sourceWallet.getId());
        SavingGoalResponseDto depRes1 = savingGoalService.depositToGoal(goal.getId(), "user1", depReq1);

        assertEquals(new BigDecimal("1000000.00"), depRes1.getCurrentAmount());
        assertEquals(new BigDecimal("50.00"), depRes1.getProgressPercentage());

        // Kiểm tra ví bị trừ 1,000,000 VND
        Wallet updatedSource = walletRepository.findById(sourceWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("4000000.00"), updatedSource.getBalance());

        // Nạp tiếp 1,000,000 VND để đạt mục tiêu 100%
        SavingGoalDepositRequest depReq2 = new SavingGoalDepositRequest(new BigDecimal("1000000.00"), sourceWallet.getId());
        SavingGoalResponseDto depRes2 = savingGoalService.depositToGoal(goal.getId(), "user1", depReq2);

        assertEquals(new BigDecimal("2000000.00"), depRes2.getCurrentAmount());
        assertEquals(new BigDecimal("100.00"), depRes2.getProgressPercentage());
        assertEquals(SavingGoalStatus.ACHIEVED, depRes2.getStatus(), "Mục tiêu đạt 100% phải chuyển trạng thái ACHIEVED");
    }

    // =========================================================================
    // PHẦN 6: TEST CÔNG CỤ TÍNH KHOẢN VAY (LOAN CALCULATOR)
    // =========================================================================

    @Test
    @DisplayName("6.1 Tính toán khoản vay ngân hàng (Dư nợ giảm dần & Niên kim cố định)")
    void testLoanCalculatorLogic() {
        LoanCalculationRequest req = new LoanCalculationRequest();
        req.setLoanTitle("Vay mua xe máy");
        req.setPrincipalAmount(new BigDecimal("50000000.00")); // 50 triệu
        req.setAnnualInterestRate(new BigDecimal("12.00"));    // 12% / năm
        req.setLoanTermMonths(12);                             // 12 tháng
        req.setStartDate(LocalDate.now());
        req.setCalculationMethod(com.finvault.module3_budget_goals.entity.LoanCalculationMethod.REDUCING_BALANCE);
        req.setSaveHistory(false);

        LoanCalculationResponseDto result = loanCalculatorService.calculateLoan("user1", req);
        assertNotNull(result);
        assertEquals(12, result.getAmortizationSchedule().size(), "Lịch trả nợ phải có đúng 12 kỳ");
        assertEquals(new BigDecimal("50000000.00"), result.getPrincipalAmount());
        assertTrue(result.getTotalInterest().compareTo(BigDecimal.ZERO) > 0, "Tổng lãi phải lớn hơn 0");
        assertTrue(result.getTotalPayable().compareTo(result.getPrincipalAmount()) > 0, "Tổng phải trả phải lớn hơn tiền gốc");

        // Kỳ cuối cùng dư nợ còn lại phải về 0
        LoanAmortizationScheduleDto lastItem = result.getAmortizationSchedule().get(11);
        assertEquals(0, lastItem.getRemainingPrincipal().compareTo(BigDecimal.ZERO), "Dư nợ kỳ cuối cùng phải về 0");
    }

    // =========================================================================
    // PHẦN 7: TEST XUẤT BÁO CÁO (EXCEL & PDF) VÀ ĐỐI SOÁT SỔ CÁI
    // =========================================================================

    @Test
    @DisplayName("7.1 Xuất file Excel và PDF sao kê giao dịch cá nhân không bị lỗi")
    void testExportExcelAndPdf() {
        byte[] excelBytes = reportService.exportTransactionsToExcel("user1", null);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0, "File Excel xuất ra không được rỗng");

        byte[] pdfBytes = reportService.exportTransactionsToPdf("user1", null);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "File PDF xuất ra không được rỗng");
    }

    @Test
    @DisplayName("7.2 Đối soát sổ cái (Reconciliation): So sánh số dư thực tế với Net Flow giao dịch")
    void testWalletReconciliation() {
        // Đối soát ví 1 (Ví tiền mặt của user1)
        ReconciliationReportDto report = reconciliationService.reconcileWallet(1L, "user1");
        assertNotNull(report);
        assertEquals(1L, report.getWalletId());
        assertNotNull(report.getActualBalance());
        assertNotNull(report.getCalculatedBalance());
        assertNotNull(report.getDiscrepancy());
    }

    @Test
    @DisplayName("7.3 Kiểm tra Smart Dynamic Excel Importer: Nhập lại file sao kê vừa xuất (Roundtrip)")
    void testDynamicExcelRoundtripImport() {
        // 1. Xuất file Excel từ hệ thống
        byte[] excelBytes = reportService.exportTransactionsToExcel("user1", null);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        // 2. Nhập lại file vừa xuất thông qua dynamic importer
        java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(excelBytes);
        com.finvault.module4_analytics_reporting.dto.ImportReportResultDto result =
                reportService.importTransactionsFromExcel("user1", 1L, bis);

        assertNotNull(result);
        assertTrue(result.getSuccessCount() >= 0);
        assertEquals(0, result.getFailureCount(), "Không được có dòng lỗi khi nhập file do chính FinVault xuất ra");
    }

    @Test
    @DisplayName("7.4 Kiểm tra Tổng quan Quỹ Gia Đình cho Gia Trưởng (family_head)")
    void testFamilyWalletsOverviewForFamilyHead() {
        // 1. Gia trưởng tra cứu danh sách các Quỹ Gia Đình
        List<WalletResponseDto> familyWallets = walletService.getFamilyWallets("family_head");
        assertNotNull(familyWallets, "Danh sách familyWallets không được null");
        assertFalse(familyWallets.isEmpty(), "Gia trưởng phải có ít nhất 1 quỹ chung gia đình (từ seed data)");

        WalletResponseDto defaultFamilyFund = familyWallets.stream()
                .filter(w -> w.getWalletType() == WalletType.FAMILY_FUND)
                .findFirst()
                .orElse(null);
        assertNotNull(defaultFamilyFund, "Phải tìm thấy ví loại FAMILY_FUND");
        assertEquals("Quỹ Tài Chính Chung Gia Đình", defaultFamilyFund.getWalletName());
        assertTrue(defaultFamilyFund.getBalance().compareTo(BigDecimal.ZERO) > 0, "Số dư quỹ gia đình phải > 0");
        assertEquals("family_head", defaultFamilyFund.getOwnerUsername());
        assertNotNull(defaultFamilyFund.getMonthlySpendingLimit());
        assertTrue(defaultFamilyFund.getMonthlySpendingLimit().compareTo(BigDecimal.ZERO) > 0);

        // 2. Gia trưởng tạo thêm một Quỹ Gia Đình mới (FAMILY_FUND)
        WalletRequest newFundReq = new WalletRequest();
        newFundReq.setWalletName("Quỹ Giáo Dục & Học Phí Con Cái");
        newFundReq.setWalletType(WalletType.FAMILY_FUND);
        newFundReq.setInitialBalance(new BigDecimal("15000000.00"));
        newFundReq.setCurrency("VND");
        newFundReq.setMonthlySpendingLimit(new BigDecimal("20000000.00"));

        WalletResponseDto createdFund = walletService.createWallet("family_head", newFundReq);
        assertNotNull(createdFund.getId());
        assertEquals(WalletType.FAMILY_FUND, createdFund.getWalletType());
        assertTrue(createdFund.getIsFamilyWallet(), "Ví FAMILY_FUND phải tự động có isFamilyWallet = true");

        // 3. Kiểm tra lại danh sách quỹ gia đình sau khi tạo mới
        List<WalletResponseDto> updatedList = walletService.getFamilyWallets("family_head");
        assertTrue(updatedList.stream().anyMatch(w -> w.getId().equals(createdFund.getId())),
                "Quỹ gia đình mới tạo phải lập tức xuất hiện trong danh sách Quỹ gia đình của Gia Trưởng");
    }
}
