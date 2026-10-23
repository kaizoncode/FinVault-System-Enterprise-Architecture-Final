package com.finvault.module3_budget_goals.service;

import com.finvault.common.BusinessException;
import com.finvault.module1_security_rbac.entity.User;
import com.finvault.module1_security_rbac.repository.UserRepository;
import com.finvault.module3_budget_goals.dto.LoanAmortizationScheduleDto;
import com.finvault.module3_budget_goals.dto.LoanCalculationRequest;
import com.finvault.module3_budget_goals.dto.LoanCalculationResponseDto;
import com.finvault.module3_budget_goals.entity.LoanCalculation;
import com.finvault.module3_budget_goals.entity.LoanCalculationMethod;
import com.finvault.module3_budget_goals.repository.LoanCalculationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * LoanCalculatorServiceImpl: Triển khai toàn diện công cụ tính toán kế hoạch trả nợ vay
 *
 * Hai phương pháp tính toán chuẩn Ngân hàng Việt Nam:
 * 1. EQUAL_INSTALLMENTS (Niên kim cố định - Trả góp đều):
 *    - Số tiền trả hàng tháng (Gốc + Lãi) không đổi trong suốt chu kỳ.
 *    - Công thức: PMT = P * [r * (1 + r)^n] / [(1 + r)^n - 1]
 * 2. REDUCING_BALANCE (Dư nợ giảm dần):
 *    - Tiền gốc trả đều hàng tháng = P / n.
 *    - Tiền lãi tính trên dư nợ thực tế còn lại = Dư nợ * (Lãi suất năm / 12).
 * ==============================================================================
 */
@Service
@Transactional
public class LoanCalculatorServiceImpl implements LoanCalculatorService {

    private static final Logger log = LoggerFactory.getLogger(LoanCalculatorServiceImpl.class);

    private final LoanCalculationRepository loanCalculationRepository;
    private final UserRepository userRepository;

    public LoanCalculatorServiceImpl(LoanCalculationRepository loanCalculationRepository,
                                     UserRepository userRepository) {
        this.loanCalculationRepository = loanCalculationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public LoanCalculationResponseDto calculateLoan(String username, LoanCalculationRequest request) {
        log.info("Thực hiện tính toán khoản vay: '{}', số tiền: {}, phương thức: {}",
                request.getLoanTitle(), request.getPrincipalAmount(), request.getCalculationMethod());

        BigDecimal principal = request.getPrincipalAmount();
        BigDecimal annualRate = request.getAnnualInterestRate();
        int months = request.getLoanTermMonths();
        LocalDate startDate = request.getStartDate();
        LoanCalculationMethod method = request.getCalculationMethod();

        List<LoanAmortizationScheduleDto> schedule = new ArrayList<>();
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal monthlyPaymentDisplay = BigDecimal.ZERO;

        if (method == LoanCalculationMethod.EQUAL_INSTALLMENTS) {
            // Niên kim cố định
            double monthlyRate = annualRate.doubleValue() / 1200.0;
            double p = principal.doubleValue();
            double pmtDouble;

            if (monthlyRate == 0) {
                pmtDouble = p / months;
            } else {
                pmtDouble = p * (monthlyRate * Math.pow(1 + monthlyRate, months)) / (Math.pow(1 + monthlyRate, months) - 1);
            }

            BigDecimal fixedMonthlyPayment = BigDecimal.valueOf(pmtDouble).setScale(2, RoundingMode.HALF_UP);
            monthlyPaymentDisplay = fixedMonthlyPayment;

            BigDecimal remainingPrincipal = principal.setScale(2, RoundingMode.HALF_UP);

            for (int k = 1; k <= months; k++) {
                LocalDate paymentDate = startDate.plusMonths(k);
                BigDecimal startingBalance = remainingPrincipal;
                BigDecimal interest = remainingPrincipal.multiply(annualRate)
                        .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);

                BigDecimal principalPaid;
                BigDecimal totalPaid;

                if (k == months) {
                    principalPaid = remainingPrincipal;
                    totalPaid = principalPaid.add(interest);
                    remainingPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                } else {
                    principalPaid = fixedMonthlyPayment.subtract(interest);
                    if (principalPaid.compareTo(remainingPrincipal) > 0) {
                        principalPaid = remainingPrincipal;
                    }
                    remainingPrincipal = remainingPrincipal.subtract(principalPaid);
                    totalPaid = principalPaid.add(interest);
                }

                totalInterest = totalInterest.add(interest);
                schedule.add(new LoanAmortizationScheduleDto(
                        k,
                        paymentDate,
                        startingBalance,
                        principalPaid,
                        interest,
                        totalPaid,
                        remainingPrincipal
                ));
            }
        } else {
            // Dư nợ giảm dần
            BigDecimal monthlyPrincipal = principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            BigDecimal remainingPrincipal = principal.setScale(2, RoundingMode.HALF_UP);

            for (int k = 1; k <= months; k++) {
                LocalDate paymentDate = startDate.plusMonths(k);
                BigDecimal startingBalance = remainingPrincipal;
                BigDecimal interest = remainingPrincipal.multiply(annualRate)
                        .divide(BigDecimal.valueOf(1200), 2, RoundingMode.HALF_UP);

                BigDecimal principalPaid;
                if (k == months) {
                    principalPaid = remainingPrincipal;
                    remainingPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                } else {
                    principalPaid = monthlyPrincipal;
                    remainingPrincipal = remainingPrincipal.subtract(principalPaid);
                }

                BigDecimal totalPaid = principalPaid.add(interest);
                totalInterest = totalInterest.add(interest);

                if (k == 1) {
                    monthlyPaymentDisplay = totalPaid; // Tháng đầu tiên trả nhiều nhất
                }

                schedule.add(new LoanAmortizationScheduleDto(
                        k,
                        paymentDate,
                        startingBalance,
                        principalPaid,
                        interest,
                        totalPaid,
                        remainingPrincipal
                ));
            }
        }

        BigDecimal totalPayable = principal.add(totalInterest);

        LoanCalculationResponseDto responseDto = new LoanCalculationResponseDto();
        responseDto.setLoanTitle(request.getLoanTitle());
        responseDto.setPrincipalAmount(principal);
        responseDto.setAnnualInterestRate(annualRate);
        responseDto.setLoanTermMonths(months);
        responseDto.setStartDate(startDate);
        responseDto.setCalculationMethod(method);
        responseDto.setMonthlyPayment(monthlyPaymentDisplay);
        responseDto.setTotalInterest(totalInterest);
        responseDto.setTotalPayable(totalPayable);
        responseDto.setNotes(request.getNotes());
        responseDto.setAmortizationSchedule(schedule);

        // Lưu lịch sử tính toán nếu có yêu cầu và người dùng đã đăng nhập
        if (Boolean.TRUE.equals(request.getSaveHistory()) && username != null && !username.isBlank()) {
            User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                    .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));

            LoanCalculation entity = new LoanCalculation(
                    user,
                    request.getLoanTitle(),
                    principal,
                    annualRate,
                    months,
                    startDate,
                    method,
                    monthlyPaymentDisplay,
                    totalInterest,
                    totalPayable,
                    request.getNotes()
            );

            LoanCalculation saved = loanCalculationRepository.save(entity);
            responseDto.setId(saved.getId());
            responseDto.setUserId(user.getId());
            responseDto.setUsername(user.getUsername());
            responseDto.setCreatedAt(saved.getCreatedAt());
            log.info("Đã lưu lịch sử tính toán khoản vay ID: {} cho user: '{}'", saved.getId(), username);
        }

        return responseDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanCalculationResponseDto> getUserLoanCalculations(String username) {
        User user = getUserByUsername(username);
        return loanCalculationRepository.findAllByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(user.getId()).stream()
                .map(LoanCalculationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LoanCalculationResponseDto getLoanCalculationById(Long id, String username) {
        LoanCalculation entity = loanCalculationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("LOAN_CALCULATION_NOT_FOUND", "Không tìm thấy bản ghi tính toán khoản vay với ID: " + id));
        validateOwnership(entity, username);
        return LoanCalculationResponseDto.fromEntity(entity);
    }

    @Override
    public void deleteLoanCalculation(Long id, String username) {
        log.info("Xóa (Soft delete) lịch sử tính toán khoản vay ID: {} bởi user: '{}'", id, username);
        LoanCalculation entity = loanCalculationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("LOAN_CALCULATION_NOT_FOUND", "Không tìm thấy bản ghi tính toán khoản vay với ID: " + id));
        validateOwnership(entity, username);

        entity.setIsDeleted(true);
        loanCalculationRepository.save(entity);
        log.info("Đã xóa mềm bản ghi tính toán khoản vay ID: {}", id);
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Không tìm thấy người dùng: " + username));
    }

    private void validateOwnership(LoanCalculation entity, String username) {
        if (entity.getUser() == null || !entity.getUser().getUsername().equals(username)) {
            throw new BusinessException("ACCESS_DENIED", "Bạn không có quyền thao tác trên bản ghi tính toán khoản vay này");
        }
    }
}
