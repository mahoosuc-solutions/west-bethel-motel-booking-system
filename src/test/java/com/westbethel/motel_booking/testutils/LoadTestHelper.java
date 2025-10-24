package com.westbethel.motel_booking.testutils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Helper utilities for load, stress, and endurance testing.
 * Provides concurrent execution, metrics collection, and result analysis.
 *
 * TDD Implementation - Agent 5, Phase 2
 */
public class LoadTestHelper {

    private final String baseUrl;
    private final RequestSpecification requestSpec;
    private final ExecutorService executorService;

    public LoadTestHelper(String baseUrl, int threadPoolSize) {
        this.baseUrl = baseUrl;
        this.requestSpec = RestAssured.given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .accept("application/json");
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * Execute concurrent requests and return performance metrics.
     */
    public LoadTestResult executeConcurrentRequests(
            List<Callable<Response>> requests,
            int concurrentUsers
    ) throws InterruptedException {
        LoadTestResult result = new LoadTestResult();
        CountDownLatch latch = new CountDownLatch(requests.size());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (Callable<Response> request : requests) {
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();
                    Response response = request.call();
                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - requestStart;

                    responseTimes.add(responseTime);

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        result.totalRequests = requests.size();
        result.successCount = successCount.get();
        result.failureCount = failureCount.get();
        result.totalDurationMs = endTime - startTime;
        result.responseTimes = responseTimes;
        result.calculateStats();

        return result;
    }

    /**
     * Execute ramped load test - gradually increase concurrent users.
     */
    public LoadTestResult executeRampedLoad(
            Callable<Response> requestTemplate,
            int startUsers,
            int maxUsers,
            int rampUpSeconds
    ) throws InterruptedException {
        LoadTestResult result = new LoadTestResult();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        int userIncrement = (maxUsers - startUsers) / rampUpSeconds;

        for (int currentUsers = startUsers; currentUsers <= maxUsers; currentUsers += userIncrement) {
            CountDownLatch latch = new CountDownLatch(currentUsers);

            for (int i = 0; i < currentUsers; i++) {
                executorService.submit(() -> {
                    try {
                        long requestStart = System.currentTimeMillis();
                        Response response = requestTemplate.call();
                        long requestEnd = System.currentTimeMillis();

                        responseTimes.add(requestEnd - requestStart);

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            Thread.sleep(1000); // 1 second between ramp steps
        }

        long endTime = System.currentTimeMillis();

        result.totalRequests = successCount.get() + failureCount.get();
        result.successCount = successCount.get();
        result.failureCount = failureCount.get();
        result.totalDurationMs = endTime - startTime;
        result.responseTimes = responseTimes;
        result.calculateStats();

        return result;
    }

    /**
     * Execute sustained load for endurance testing.
     */
    public LoadTestResult executeSustainedLoad(
            Callable<Response> requestTemplate,
            int concurrentUsers,
            int durationSeconds
    ) throws InterruptedException {
        LoadTestResult result = new LoadTestResult();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicInteger requestCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds * 1000L);

        // Submit continuous load
        for (int i = 0; i < concurrentUsers; i++) {
            executorService.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        long requestStart = System.currentTimeMillis();
                        Response response = requestTemplate.call();
                        long requestEnd = System.currentTimeMillis();

                        requestCount.incrementAndGet();
                        responseTimes.add(requestEnd - requestStart);

                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }

                        Thread.sleep(100); // Small delay between requests
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                }
            });
        }

        // Wait for duration
        Thread.sleep(durationSeconds * 1000L);

        result.totalRequests = requestCount.get();
        result.successCount = successCount.get();
        result.failureCount = failureCount.get();
        result.totalDurationMs = System.currentTimeMillis() - startTime;
        result.responseTimes = responseTimes;
        result.calculateStats();

        return result;
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    /**
     * Container for load test results and statistics.
     */
    public static class LoadTestResult {
        public int totalRequests;
        public int successCount;
        public int failureCount;
        public long totalDurationMs;
        public List<Long> responseTimes = new ArrayList<>();

        // Calculated statistics
        public double averageResponseTime;
        public long minResponseTime;
        public long maxResponseTime;
        public long medianResponseTime;
        public long p95ResponseTime;
        public long p99ResponseTime;
        public double throughput; // requests per second
        public double successRate;

        public void calculateStats() {
            if (responseTimes.isEmpty()) {
                return;
            }

            List<Long> sorted = responseTimes.stream()
                    .sorted()
                    .collect(Collectors.toList());

            averageResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            minResponseTime = sorted.get(0);
            maxResponseTime = sorted.get(sorted.size() - 1);
            medianResponseTime = sorted.get(sorted.size() / 2);

            int p95Index = (int) (sorted.size() * 0.95);
            int p99Index = (int) (sorted.size() * 0.99);

            p95ResponseTime = sorted.get(Math.min(p95Index, sorted.size() - 1));
            p99ResponseTime = sorted.get(Math.min(p99Index, sorted.size() - 1));

            throughput = (totalRequests * 1000.0) / totalDurationMs;
            successRate = (successCount * 100.0) / totalRequests;
        }

        @Override
        public String toString() {
            return String.format(
                    "LoadTestResult{" +
                            "totalRequests=%d, " +
                            "success=%d (%.2f%%), " +
                            "failures=%d, " +
                            "duration=%dms, " +
                            "throughput=%.2f req/s, " +
                            "avgResponse=%.2fms, " +
                            "min=%dms, " +
                            "max=%dms, " +
                            "median=%dms, " +
                            "p95=%dms, " +
                            "p99=%dms" +
                            "}",
                    totalRequests,
                    successCount, successRate,
                    failureCount,
                    totalDurationMs,
                    throughput,
                    averageResponseTime,
                    minResponseTime,
                    maxResponseTime,
                    medianResponseTime,
                    p95ResponseTime,
                    p99ResponseTime
            );
        }

        public boolean isSuccessful() {
            return successRate >= 95.0 && p95ResponseTime < 2000; // 95% success, p95 < 2s
        }
    }
}
