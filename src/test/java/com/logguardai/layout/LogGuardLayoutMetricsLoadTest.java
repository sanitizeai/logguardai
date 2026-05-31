package com.logguardai.layout;

import com.logguardai.metrics.MetricsConfig;
import com.logguardai.metrics.MetricsPattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Load test for the LogGuardLayout metrics extraction path.
 * Generates at least 150k log messages per second and validates throughput.
 */
public class LogGuardLayoutMetricsLoadTest {

    private LogGuardLayout layout;
    private File metricsFile;
    private ExecutorService executor;

    @Before
    public void setUp() {
        String metricsFilePath = "target/load_metrics.txt";
        metricsFile = new File(metricsFilePath);
        if (metricsFile.exists()) {
            metricsFile.delete();
        }

        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setEnabled(true);
        metricsConfig.setFilePath(metricsFilePath);
        metricsConfig.setFlushIntervalMs(10_000); // flush only at the end of the test
        metricsConfig.setMaxCardinalityPerPattern(100_000);

        List<String> fields = new ArrayList<>();
        fields.add("method");
        fields.add("path");
        fields.add("status");
        MetricsPattern pattern = new MetricsPattern(
                "http_requests",
                "(GET|POST|PUT|DELETE)\\s+([/\\w/-]+)\\s+(\\d{3})",
                "http_requests_total",
                fields);
        metricsConfig.addPattern(pattern);

        layout = new LogGuardLayout(
                StandardCharsets.UTF_8,
                false,
                true,
                null,
                5,
                100,
                5,
                0.05,
                new ArrayList<>(),
                metricsConfig
        );

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors());
        executor = Executors.newFixedThreadPool(threadCount);
    }

    @After
    public void tearDown() {
        if (layout != null) {
            layout.flushMetrics();
            layout.shutdown();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        //if (metricsFile != null && metricsFile.exists()) {
        //   metricsFile.delete();
        //}
    }

    @Test
    public void testLogGuardLayoutMetricsLoad() throws Exception {
        final int totalMessages = 150_000;
        final CountDownLatch latch = new CountDownLatch(totalMessages);
        final AtomicBoolean failed = new AtomicBoolean(false);

        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.LoadTest")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("GET /api/load-test 200"))
                .setTimeMillis(System.currentTimeMillis())
                .build();

        long startNanos = System.nanoTime();
        for (int i = 0; i < totalMessages; i++) {
            executor.submit(() -> {
                try {
                    layout.toSerializable(event);
                } catch (Exception e) {
                    failed.set(true);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long elapsedNanos = System.nanoTime() - startNanos;
        double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
        double throughput = totalMessages / elapsedSeconds;

        if (!completed) {
            fail("Load test did not complete within 30 seconds: processed=" + (totalMessages - latch.getCount()));
        }
        if (failed.get()) {
            fail("One or more log events failed during processing.");
        }

        System.out.println("Load test throughput: " + String.format("%.0f", throughput) + " logs/sec");
        assertTrue("Expected throughput of at least 150k logs/sec, but got " + String.format("%.0f", throughput),
                throughput >= 150_000);
    }
}
