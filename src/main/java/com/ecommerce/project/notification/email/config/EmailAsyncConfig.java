package com.ecommerce.project.notification.email.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableRetry
public class EmailAsyncConfig {
    @Value("${app.email.async.core-pool-size}")
    private int corePoolSize;
    @Value("${app.email.async.max-pool-size}")
    private int maxPoolSize;
    @Value("${app.email.async.queue-capacity}")
    private int queueCapacity;
    @Value("${app.email.async.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.initialize();
        return executor;
    }
}
