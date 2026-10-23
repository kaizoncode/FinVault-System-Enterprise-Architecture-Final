package com.finvault.module3_budget_goals.scheduler;

import com.finvault.module3_budget_goals.dto.BudgetAlertNotificationDto;
import com.finvault.module3_budget_goals.entity.SavingGoal;
import com.finvault.module3_budget_goals.repository.SavingGoalRepository;
import com.finvault.module3_budget_goals.websocket.BudgetAlertWebSocketSender;
import com.finvault.module5_admin_audit.entity.SystemNotification;
import com.finvault.module5_admin_audit.repository.SystemNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ==============================================================================
 * [Owner: Member 3 - Budget & Financial Goals Module]
 * SavingGoalProgressScheduler: Tác vụ định kỳ kiểm tra tiến độ các Quỹ mục tiêu tiết kiệm
 *
 * Lịch trình thực thi:
 * - Chạy định kỳ vào 08:00 sáng mỗi ngày (Cron: 0 0 8 * * ?)
 * - Rà soát các mục tiêu tiết kiệm sắp đến hạn trong vòng 30 ngày để ghi nhận và cảnh báo qua WebSocket.
 * ==============================================================================
 */
@Component
public class SavingGoalProgressScheduler {

    private static final Logger log = LoggerFactory.getLogger(SavingGoalProgressScheduler.class);

    private final SavingGoalRepository savingGoalRepository;
    private final BudgetAlertWebSocketSender webSocketSender;
    private final SystemNotificationRepository systemNotificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SavingGoalProgressScheduler(SavingGoalRepository savingGoalRepository,
                                       BudgetAlertWebSocketSender webSocketSender,
                                       SystemNotificationRepository systemNotificationRepository,
                                       SimpMessagingTemplate messagingTemplate) {
        this.savingGoalRepository = savingGoalRepository;
        this.webSocketSender = webSocketSender;
        this.systemNotificationRepository = systemNotificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Quét các mục tiêu tiết kiệm sắp đến hạn trong vòng 30 ngày
     */
    @Scheduled(cron = "${finvault.goals.check-cron:0 0 8 * * ?}")
    @Transactional
    public void checkUpcomingSavingGoals() {
        LocalDate today = LocalDate.now();
        LocalDate upcoming30Days = today.plusDays(30);

        log.info("Bắt đầu quét các Mục tiêu tiết kiệm sắp đến hạn (Từ {} đến {})", today, upcoming30Days);

        List<SavingGoal> upcomingGoals = savingGoalRepository.findUpcomingDueGoals(today, upcoming30Days);
        if (upcomingGoals.isEmpty()) {
            log.info("Không có mục tiêu tiết kiệm nào sắp đến hạn trong 30 ngày tới.");
            return;
        }

        for (SavingGoal goal : upcomingGoals) {
            log.info("Mục tiêu tiết kiệm [{}] của user [{}] sắp đến hạn vào ngày {}. Tiến độ hiện tại: {}% ({} / {})",
                    goal.getGoalName(),
                    goal.getUser().getUsername(),
                    goal.getTargetDate(),
                    goal.getProgressPercentage(),
                    goal.getCurrentAmount(),
                    goal.getTargetAmount());

            // Gửi cảnh báo tiến độ thời gian thực qua WebSocket & lưu thông báo riêng tư vào DB chỉ cho goal owner
            String alertMessage = String.format("Mục tiêu tiết kiệm '%s' sắp đến hạn (%s). Tiến độ hiện tại: %s%%",
                    goal.getGoalName(), goal.getTargetDate(), goal.getProgressPercentage().toPlainString());

            if (goal.getUser() != null) {
                BudgetAlertNotificationDto alert = new BudgetAlertNotificationDto(
                        goal.getId(),
                        goal.getGoalName(),
                        "Mục Tiêu Tiết Kiệm",
                        BudgetAlertNotificationDto.AlertSeverity.WARNING_80,
                        goal.getTargetAmount(),
                        goal.getCurrentAmount(),
                        goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO),
                        goal.getProgressPercentage(),
                        alertMessage
                );

                // Gửi WebSocket riêng tư qua convertAndSendToUser chỉ cho goal owner
                if (messagingTemplate != null) {
                    try {
                        messagingTemplate.convertAndSendToUser(goal.getUser().getUsername(), "/queue/saving-goals", alert);
                    } catch (Exception ex) {
                        log.error("Không thể gửi WebSocket tới user [{}]: {}", goal.getUser().getUsername(), ex.getMessage());
                    }
                } else if (webSocketSender != null) {
                    webSocketSender.sendAlertToUser(goal.getUser().getUsername(), alert);
                }
            }

            if (systemNotificationRepository != null && goal.getUser() != null) {
                try {
                    SystemNotification notification = new SystemNotification(
                            "Nhắc nhở mục tiêu: " + goal.getGoalName(),
                            alertMessage,
                            "WARNING",
                            true,
                            null,
                            goal.getUser().getId(),
                            goal.getUser().getUsername(),
                            "SYSTEM_SCHEDULER"
                    );
                    notification.setBroadcastToRoles(null);
                    systemNotificationRepository.save(notification);
                } catch (Exception ex) {
                    log.error("Không thể lưu thông báo hệ thống cho mục tiêu [{}]: {}", goal.getGoalName(), ex.getMessage());
                }
            }
        }
    }
}
