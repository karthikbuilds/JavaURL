package com.karthik.JavaURL.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Dedicated worker pool for fire-and-forget tasks such as storing click analytics,
 * keeping them off the request thread so redirects stay fast.
 */
@Configuration
public class TaskExecutorConfig {

    @Bean(name = "clickRecordExecutor")
    public Executor clickRecordExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(5000);
        executor.setThreadNamePrefix("clicks-");
        executor.initialize();
        return executor;
    }
}