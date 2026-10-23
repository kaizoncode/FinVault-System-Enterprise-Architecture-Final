package com.finvault.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ==============================================================================
 * [Owner: Architecture Core / All Members]
 * AsyncConfig: Cấu hình ThreadPoolTaskExecutor phục vụ xử lý bất đồng bộ
 * (Ghi Audit Log không gây nghẽn luồng giao dịch, gửi WebSocket Alerts & Batch Analytics)
 *
 * Điểm chờ nâng cấp (Extension Point):
 * - Tích hợp Distributed Task Queue (RabbitMQ / Apache Kafka)
 * ==============================================================================
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("FinVaultAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("Ngoại lệ bất đồng bộ không được bắt tại phương thức [{}]: ", method.getName(), throwable);
        };
    }
}
