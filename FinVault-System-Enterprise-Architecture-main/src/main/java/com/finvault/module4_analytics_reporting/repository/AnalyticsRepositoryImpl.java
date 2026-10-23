package com.finvault.module4_analytics_reporting.repository;

import com.finvault.module2_wallet_ledger.entity.ApprovalStatus;
import com.finvault.module2_wallet_ledger.entity.Transaction;
import com.finvault.module2_wallet_ledger.entity.TransactionType;
import com.finvault.module4_analytics_reporting.dto.CashFlowTrendDto;
import com.finvault.module4_analytics_reporting.dto.CategoryExpenseDistributionDto;
import com.finvault.module4_analytics_reporting.dto.TransactionExportFilterRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ==============================================================================
 * [Owner: Member 4 - Analytics, BI & Reporting Module]
 * AnalyticsRepositoryImpl: Triển khai các hàm truy vấn phân tích dữ liệu độc lập Database (Database-Agnostic)
 *
 * Tính tương thích cao:
 * - Thay vì dùng hàm ngày tháng riêng biệt của MySQL/Postgres, phân tích nhóm dữ liệu
 *   bằng Java Stream đảm bảo hoạt động trơn tru cả trên H2 In-Memory và Production RDBMS.
 * ==============================================================================
 */
@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CashFlowTrendDto.CashFlowDataPoint> getDailyCashFlowTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        String jpql = "SELECT t FROM Transaction t WHERE t.isDeleted = false AND t.approvalStatus = :status " +
                      "AND t.transactionDate >= :startDateTime AND t.transactionDate <= :endDateTime ";
        if (userId != null) {
            jpql += "AND t.user.id = :userId ";
        }
        jpql += "ORDER BY t.transactionDate ASC";

        TypedQuery<Transaction> query = entityManager.createQuery(jpql, Transaction.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .setParameter("startDateTime", startDateTime)
                .setParameter("endDateTime", endDateTime);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        List<Transaction> transactions = query.getResultList();

        // Khởi tạo map cho tất cả các ngày trong khoảng thời gian để đảm bảo biểu đồ không bị khuyết ngày
        Map<LocalDate, BigDecimal[]> dailyMap = new TreeMap<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            dailyMap.put(cur, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            cur = cur.plusDays(1);
        }

        for (Transaction tx : transactions) {
            LocalDate txDate = tx.getTransactionDate().toLocalDate();
            if (dailyMap.containsKey(txDate)) {
                BigDecimal[] flows = dailyMap.get(txDate);
                if (tx.getTransactionType() == TransactionType.INCOME) {
                    flows[0] = flows[0].add(tx.getAmount());
                } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                    flows[1] = flows[1].add(tx.getAmount());
                }
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        List<CashFlowTrendDto.CashFlowDataPoint> result = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal[]> entry : dailyMap.entrySet()) {
            BigDecimal in = entry.getValue()[0];
            BigDecimal out = entry.getValue()[1];
            BigDecimal net = in.subtract(out);
            result.add(new CashFlowTrendDto.CashFlowDataPoint(
                    entry.getKey().format(formatter),
                    entry.getKey(),
                    in,
                    out,
                    net
            ));
        }
        return result;
    }

    @Override
    public List<CashFlowTrendDto.CashFlowDataPoint> getMonthlyCashFlowTrend(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        String jpql = "SELECT t FROM Transaction t WHERE t.isDeleted = false AND t.approvalStatus = :status " +
                      "AND t.transactionDate >= :startDateTime AND t.transactionDate <= :endDateTime ";
        if (userId != null) {
            jpql += "AND t.user.id = :userId ";
        }
        jpql += "ORDER BY t.transactionDate ASC";

        TypedQuery<Transaction> query = entityManager.createQuery(jpql, Transaction.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .setParameter("startDateTime", startDateTime)
                .setParameter("endDateTime", endDateTime);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        List<Transaction> transactions = query.getResultList();

        Map<YearMonth, BigDecimal[]> monthlyMap = new TreeMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyMap.put(YearMonth.of(year, m), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Transaction tx : transactions) {
            YearMonth ym = YearMonth.from(tx.getTransactionDate());
            if (monthlyMap.containsKey(ym)) {
                BigDecimal[] flows = monthlyMap.get(ym);
                if (tx.getTransactionType() == TransactionType.INCOME) {
                    flows[0] = flows[0].add(tx.getAmount());
                } else if (tx.getTransactionType() == TransactionType.EXPENSE) {
                    flows[1] = flows[1].add(tx.getAmount());
                }
            }
        }

        List<CashFlowTrendDto.CashFlowDataPoint> result = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal[]> entry : monthlyMap.entrySet()) {
            BigDecimal in = entry.getValue()[0];
            BigDecimal out = entry.getValue()[1];
            BigDecimal net = in.subtract(out);
            String label = "Tháng " + entry.getKey().getMonthValue();
            result.add(new CashFlowTrendDto.CashFlowDataPoint(
                    label,
                    entry.getKey().atDay(1),
                    in,
                    out,
                    net
            ));
        }
        return result;
    }

    @Override
    public List<CategoryExpenseDistributionDto.CategoryExpenseItem> getCategoryExpenseDistribution(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        String jpql = "SELECT t FROM Transaction t WHERE t.isDeleted = false AND t.approvalStatus = :status " +
                      "AND t.transactionType = :txType " +
                      "AND t.transactionDate >= :startDateTime AND t.transactionDate <= :endDateTime ";
        if (userId != null) {
            jpql += "AND t.user.id = :userId ";
        }

        TypedQuery<Transaction> query = entityManager.createQuery(jpql, Transaction.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .setParameter("txType", TransactionType.EXPENSE)
                .setParameter("startDateTime", startDateTime)
                .setParameter("endDateTime", endDateTime);
        if (userId != null) {
            query.setParameter("userId", userId);
        }

        List<Transaction> transactions = query.getResultList();
        BigDecimal totalExpense = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalExpense.compareTo(BigDecimal.ZERO) <= 0) {
            return Collections.emptyList();
        }

        Map<Long, List<Transaction>> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(tx -> (tx.getCategory() != null) ? tx.getCategory().getId() : -1L));

        List<CategoryExpenseDistributionDto.CategoryExpenseItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : byCategory.entrySet()) {
            BigDecimal categorySum = entry.getValue().stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal percentage = categorySum.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpense, 2, RoundingMode.HALF_UP);

            Transaction sample = entry.getValue().get(0);
            String catName = (sample.getCategory() != null) ? sample.getCategory().getCategoryName() : "Khác / Chưa phân loại";
            String icon = (sample.getCategory() != null && sample.getCategory().getIcon() != null) ? sample.getCategory().getIcon() : "bi-tag";
            String color = (sample.getCategory() != null && sample.getCategory().getColor() != null) ? sample.getCategory().getColor() : "#6c757d";

            items.add(new CategoryExpenseDistributionDto.CategoryExpenseItem(
                    entry.getKey(),
                    catName,
                    icon,
                    color,
                    categorySum,
                    percentage
            ));
        }

        // Sắp xếp giảm dần theo số tiền chi tiêu
        items.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return items;
    }

    @Override
    public List<Transaction> findTransactionsForExport(Long userId, TransactionExportFilterRequest filter) {
        StringBuilder jpql = new StringBuilder("SELECT t FROM Transaction t WHERE t.isDeleted = false ");
        Map<String, Object> params = new HashMap<>();

        if (userId != null) {
            jpql.append("AND (t.user.id = :userId OR t.wallet.user.id = :userId OR EXISTS (SELECT wm FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId AND wm.isActive = true)) ");
            params.put("userId", userId);
        }

        if (filter != null) {
            if (filter.getWalletId() != null) {
                jpql.append("AND t.wallet.id = :walletId ");
                params.put("walletId", filter.getWalletId());
            }
            if (filter.getCategoryId() != null) {
                jpql.append("AND t.category.id = :categoryId ");
                params.put("categoryId", filter.getCategoryId());
            }
            if (filter.getTransactionType() != null) {
                jpql.append("AND t.transactionType = :txType ");
                params.put("txType", filter.getTransactionType());
            }
            if (filter.getStartDate() != null) {
                jpql.append("AND t.transactionDate >= :startDateTime ");
                params.put("startDateTime", filter.getStartDate().atStartOfDay());
            }
            if (filter.getEndDate() != null) {
                jpql.append("AND t.transactionDate <= :endDateTime ");
                params.put("endDateTime", filter.getEndDate().atTime(LocalTime.MAX));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                jpql.append("AND LOWER(t.note) LIKE :keyword ");
                params.put("keyword", "%" + filter.getKeyword().trim().toLowerCase() + "%");
            }
        }

        jpql.append("ORDER BY t.transactionDate DESC");

        TypedQuery<Transaction> query = entityManager.createQuery(jpql.toString(), Transaction.class);
        for (Map.Entry<String, Object> p : params.entrySet()) {
            query.setParameter(p.getKey(), p.getValue());
        }

        return query.getResultList();
    }

    @Override
    public BigDecimal sumTotalIncome(Long userId, LocalDate startDate, LocalDate endDate) {
        String jpql = "SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.isDeleted = false " +
                      "AND t.approvalStatus = :status AND t.transactionType = :type " +
                      "AND t.transactionDate >= :startDateTime AND t.transactionDate <= :endDateTime ";
        if (userId != null) {
            jpql += "AND t.user.id = :userId ";
        }

        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .setParameter("type", TransactionType.INCOME)
                .setParameter("startDateTime", startDate.atStartOfDay())
                .setParameter("endDateTime", endDate.atTime(LocalTime.MAX));
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.getSingleResult();
    }

    @Override
    public BigDecimal sumTotalExpense(Long userId, LocalDate startDate, LocalDate endDate) {
        String jpql = "SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.isDeleted = false " +
                      "AND t.approvalStatus = :status AND t.transactionType = :type " +
                      "AND t.transactionDate >= :startDateTime AND t.transactionDate <= :endDateTime ";
        if (userId != null) {
            jpql += "AND t.user.id = :userId ";
        }

        TypedQuery<BigDecimal> query = entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .setParameter("type", TransactionType.EXPENSE)
                .setParameter("startDateTime", startDate.atStartOfDay())
                .setParameter("endDateTime", endDate.atTime(LocalTime.MAX));
        if (userId != null) {
            query.setParameter("userId", userId);
        }
        return query.getSingleResult();
    }

    @Override
    public BigDecimal sumSystemTotalVolume() {
        return entityManager.createQuery(
                "SELECT COALESCE(SUM(t.amount), 0.00) FROM Transaction t WHERE t.isDeleted = false AND t.approvalStatus = :status",
                BigDecimal.class)
                .setParameter("status", ApprovalStatus.APPROVED)
                .getSingleResult();
    }

    @Override
    public BigDecimal sumSystemTotalAssets() {
        return entityManager.createQuery(
                "SELECT COALESCE(SUM(w.balance), 0.00) FROM Wallet w WHERE w.isDeleted = false AND w.isActive = true",
                BigDecimal.class)
                .getSingleResult();
    }

    @Override
    public Long countSystemTotalTransactions() {
        return entityManager.createQuery(
                "SELECT COUNT(t) FROM Transaction t WHERE t.isDeleted = false",
                Long.class)
                .getSingleResult();
    }
}
