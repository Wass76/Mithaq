package com.Shakwa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous email sending.
 * Provides a dedicated thread pool executor to prevent blocking HTTP requests
 * while emails are being sent.
 */
@Configuration
@EnableAsync
public class EmailAsyncConfig {

    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // Minimum threads always alive
        executor.setMaxPoolSize(5);           // Maximum threads for peak load
        executor.setQueueCapacity(100);       // Queue size before rejection
        executor.setThreadNamePrefix("email-dispatch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
