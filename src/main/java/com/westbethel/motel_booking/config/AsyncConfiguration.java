package com.westbethel.motel_booking.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Processing Configuration
 *
 * Enables asynchronous method execution for:
 * - Email sending
 * - Audit logging
 * - Report generation
 * - Cache warming
 * - Notification processing
 *
 * Performance Impact:
 * - Offloads long-running tasks from request threads
 * - Improves response times for user-facing operations
 * - Enables parallel processing of independent tasks
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration implements AsyncConfigurer {

    /**
     * Primary task executor for async operations
     *
     * Pool Configuration:
     * - Core Pool Size: 5 threads (always alive)
     * - Max Pool Size: 10 threads (scales under load)
     * - Queue Capacity: 100 tasks (overflow buffer)
     *
     * Tuning Guidance:
     * - Core pool = minimum threads for baseline load
     * - Max pool = 2x core pool for burst capacity
     * - Queue capacity = expected peak concurrent async tasks
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool: Always-alive threads
        executor.setCorePoolSize(5);

        // Max pool: Scale up to this limit under load
        executor.setMaxPoolSize(10);

        // Queue capacity: Buffer for tasks when all threads busy
        executor.setQueueCapacity(100);

        // Thread naming for debugging
        executor.setThreadNamePrefix("async-");

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Rejection policy: Log and run in caller thread
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("Async task rejected, running in caller thread: {}", r);
            r.run();
        });

        executor.initialize();
        return executor;
    }

    /**
     * Email-specific executor for notification tasks
     *
     * Separate pool for email operations to:
     * - Isolate email failures from other async tasks
     * - Tune specifically for email sending patterns
     * - Prevent email backlog from blocking other async operations
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Email-specific tuning
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200); // Higher queue for email bursts

        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // Longer wait for email completion

        // Rejection policy: Log and queue for retry
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.error("Email task rejected, email may need manual retry: {}", r);
        });

        executor.initialize();
        return executor;
    }

    /**
     * Audit-specific executor for audit logging
     *
     * Dedicated pool for audit operations to:
     * - Ensure audit logs are not lost due to thread pool saturation
     * - Maintain audit trail integrity
     * - Isolate audit performance from other operations
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Audit-specific tuning
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(500); // Large queue for audit bursts

        executor.setThreadNamePrefix("audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(180); // Ensure audits complete

        // Rejection policy: Log critical error
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.error("CRITICAL: Audit task rejected, audit log may be lost: {}", r);
        });

        executor.initialize();
        return executor;
    }

    /**
     * Report generation executor for heavy computation
     *
     * Configured for CPU-intensive report generation tasks
     */
    @Bean(name = "reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Report-specific tuning (fewer threads for CPU-intensive work)
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);

        executor.setThreadNamePrefix("report-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300); // Long-running reports

        executor.initialize();
        return executor;
    }

    /**
     * Exception handler for uncaught async exceptions
     *
     * Logs exceptions that occur in async methods to prevent silent failures
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("Uncaught async exception in method: {}.{}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                throwable);

            // Log method parameters for debugging
            if (params != null && params.length > 0) {
                log.error("Method parameters: {}", (Object[]) params);
            }
        };
    }
}
