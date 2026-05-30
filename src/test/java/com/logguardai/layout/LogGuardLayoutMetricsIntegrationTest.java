package com.logguardai.layout;

import com.logguardai.metrics.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration test for metrics recording in LogGuardLayout.
 */
public class LogGuardLayoutMetricsIntegrationTest {

    private LogGuardLayout layout;
    private MetricsConfig metricsConfig;
    private String metricsFilePath;

    @Before
    public void setUp() {
        metricsFilePath = "target/test_layout_metrics.txt";
        new File("target").mkdirs();
        
        // Create metrics config with HTTP and FTP patterns
        metricsConfig = new MetricsConfig();
        metricsConfig.setEnabled(true);
        metricsConfig.setFilePath(metricsFilePath);
        metricsConfig.setFlushIntervalMs(1000);  // 1 second
        metricsConfig.setMaxCardinalityPerPattern(1000);
        
        // Add HTTP pattern
        List<String> httpFields = new ArrayList<>();
        httpFields.add("method");
        httpFields.add("status");
        MetricsPattern httpPattern = new MetricsPattern("http_requests", 
                "(GET|POST|PUT|DELETE)\\s+([/\\w/]+)\\s+.*?(\\d{3})", 
                "http_requests_total", 
                httpFields);
        metricsConfig.addPattern(httpPattern);
        
        // Create layout with metrics enabled
        layout = new LogGuardLayout(
                StandardCharsets.UTF_8,
                false,  // aiEnabled
                true,   // extractMetrics
                null,   // aiConfig
                5,      // aiThreshold
                100,    // aiAsyncWaitMs
                5,      // batchSize
                0.05,   // samplingRate
                new ArrayList<>(),  // safeKeyPatterns
                metricsConfig
        );
    }

    @After
    public void tearDown() {
        // Shutdown layout and flush metrics
        layout.flushMetrics();
        layout.shutdown();
        
        // Cleanup
        File metricsFile = new File(metricsFilePath);
        if (metricsFile.exists()) {
            metricsFile.delete();
        }
    }

    @Test
    public void testMetricsRecordingWithLogGuardLayout() throws InterruptedException {
        // Create log events
        long timeMillis = System.currentTimeMillis();
        
        // HTTP GET 200
        Log4jLogEvent event1 = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("GET /api/users 200"))
                .setTimeMillis(timeMillis)
                .build();
        
        // HTTP GET 200 again
        Log4jLogEvent event2 = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("GET /api/users 200"))
                .setTimeMillis(timeMillis + 10)
                .build();
        
        // HTTP POST 201
        Log4jLogEvent event3 = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("POST /api/orders 201"))
                .setTimeMillis(timeMillis + 20)
                .build();
        
        // Process events through layout
        String output1 = layout.toSerializable(event1);
        String output2 = layout.toSerializable(event2);
        String output3 = layout.toSerializable(event3);
        
        assertNotNull("Layout should format events", output1);
        assertTrue("Output should contain logger name", output1.contains("com.example.App"));
        
        // Flush metrics
        layout.flushMetrics();
        
        // Wait for file write
        Thread.sleep(500);
        
        // Verify metrics were recorded
        MetricsRegistry registry = layout.getMetricsRegistry();
        assertNotNull("Metrics registry should be available", registry);
        assertTrue("Should have recorded metrics", registry.getMetricsCount() > 0);
    }

    @Test
    public void testMetricsFileOutput() throws InterruptedException, IOException {
        // Log some HTTP requests
        long timeMillis = System.currentTimeMillis();
        
        for (int i = 0; i < 5; i++) {
            Log4jLogEvent event = Log4jLogEvent.newBuilder()
                    .setLoggerName("com.example.App")
                    .setLevel(Level.INFO)
                    .setMessage(new SimpleMessage("GET /api/users 200"))
                    .setTimeMillis(timeMillis + i * 10)
                    .build();
            layout.toSerializable(event);
        }
        
        // Flush metrics to file
        layout.flushMetrics();
        
        // Wait for file write
        Thread.sleep(500);
        
        // Verify file exists and contains metrics
        File metricsFile = new File(metricsFilePath);
        assertTrue("Metrics file should exist", metricsFile.exists());
        
        String content = new String(Files.readAllBytes(Paths.get(metricsFilePath)));
        assertFalse("Metrics file should not be empty", content.isEmpty());
        assertTrue("File should contain metric name", content.contains("http_requests_total"));
        assertTrue("File should contain GET method label", content.contains("GET") || content.contains("method="));
    }

    @Test
    public void testMetricsFlushManagerLifecycle() throws InterruptedException {
        MetricsFlushManager manager = layout.getMetricsFlushManager();
        assertNotNull("Flush manager should be initialized", manager);
        assertTrue("Manager should be running after layout creation", manager.isRunning());
        
        // Record a metric
        long timeMillis = System.currentTimeMillis();
        Log4jLogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("GET /api/users 200"))
                .setTimeMillis(timeMillis)
                .build();
        layout.toSerializable(event);
        
        // Manual flush
        manager.flush();
        
        // Verify file was created
        File metricsFile = new File(metricsFilePath);
        assertTrue("Metrics file should exist after flush", metricsFile.exists());
    }

    @Test
    public void testMultiplePatternMatching() throws InterruptedException {
        // Add another pattern to config
        List<String> ftpFields = new ArrayList<>();
        ftpFields.add("operation");
        MetricsPattern ftpPattern = new MetricsPattern("ftp_transfers",
                "(RECV|SEND)\\s+([\\w\\.]+)",
                "ftp_transfers_total",
                ftpFields);
        
        metricsConfig.addPattern(ftpPattern);
        layout.getMetricsRegistry().addPattern(ftpPattern);
        
        // Log both HTTP and FTP
        long timeMillis = System.currentTimeMillis();
        
        Log4jLogEvent httpEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("GET /api/users 200"))
                .setTimeMillis(timeMillis)
                .build();
        
        Log4jLogEvent ftpEvent = Log4jLogEvent.newBuilder()
                .setLoggerName("com.example.App")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("RECV backup.zip"))
                .setTimeMillis(timeMillis + 10)
                .build();
        
        layout.toSerializable(httpEvent);
        layout.toSerializable(ftpEvent);
        
        layout.flushMetrics();
        Thread.sleep(500);
        
        // Should have metrics from both patterns
        MetricsRegistry registry = layout.getMetricsRegistry();
        int metricsCount = registry.getMetricsCount();
        assertTrue("Should have recorded metrics from both patterns", metricsCount >= 2);
    }

    @Test
    public void testLayoutCreationWithFactory() {
        // Test the createLayout factory method with metrics parameters
        String metricsPatterns = "http_req|GET\\s+([/\\w/]+)\\s+(\\d{3})|http_requests_total|endpoint,status";
        
        LogGuardLayout factoryLayout = LogGuardLayout.createLayout(
                StandardCharsets.UTF_8,
                false,  // aiEnabled
                "openai",  // aiProvider
                "",  // aiApiKey
                "gpt-3.5-turbo",  // aiModel
                "",  // azureEndpoint
                "",  // azureDeployment
                "2023-12-01",  // azureApiVersion
                true,  // extractMetrics
                "target/factory_metrics.txt",  // metricsFilePath
                5000,  // metricsFlushIntervalMs
                1000,  // metricsMaxCardinality
                metricsPatterns,  // metricsPatterns
                5,  // aiThreshold
                2000,  // aiTimeoutMs
                100,  // aiAsyncWaitMs
                5,  // batchSize
                0.05,  // samplingRate
                ""  // safeKeyPatterns
        );
        
        assertNotNull("Factory should create layout", factoryLayout);
        assertNotNull("Layout should have metrics registry", factoryLayout.getMetricsRegistry());
        
        // Verify pattern was parsed
        int patternCount = factoryLayout.getMetricsRegistry().getPatternCount();
        assertEquals("Should have 1 pattern from factory definition", 1, patternCount);
        
        // Cleanup
        factoryLayout.shutdown();
        File metricsFile = new File("target/factory_metrics.txt");
        if (metricsFile.exists()) {
            metricsFile.delete();
        }
    }
}
