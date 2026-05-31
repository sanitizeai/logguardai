package com.logguardai.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for pattern-based metrics system.
 */
public class MetricsPatternTest {

    private MetricsRegistry registry;
    private MetricsPattern httpPattern;
    private MetricsPattern ftpPattern;

    @Before
    public void setUp() {
        registry = new MetricsRegistry(1000);
        
        // HTTP GET pattern: extract method, endpoint, status code
        List<String> httpFields = new ArrayList<>();
        httpFields.add("method");
        httpFields.add("endpoint");
        httpFields.add("status");
        httpPattern = new MetricsPattern("http_requests", 
                "(GET|POST|PUT|DELETE)\\s+([/\\w\\-]+)\\s+.*?(\\d{3})", 
                "http_requests_total", 
                httpFields);
        
        // FTP pattern: extract operation, file
        List<String> ftpFields = new ArrayList<>();
        ftpFields.add("operation");
        ftpFields.add("filename");
        ftpPattern = new MetricsPattern("ftp_transfers",
                "(RECV|SEND|DELETE)\\s+([\\w\\.]+)",
                "ftp_transfers_total",
                ftpFields);
    }

    @Test
    public void testMetricsPatternMatching() {
        // Test HTTP pattern
        Map<String, String> labels = httpPattern.match("GET /api/users 200");
        assertNotNull("Pattern should match HTTP GET request", labels);
        assertEquals("Method should be GET", "GET", labels.get("method"));
        assertEquals("Endpoint should be /api/users", "/api/users", labels.get("endpoint"));
        assertEquals("Status should be 200", "200", labels.get("status"));

        // Test FTP pattern
        labels = ftpPattern.match("RECV data.csv");
        assertNotNull("Pattern should match FTP RECV", labels);
        assertEquals("Operation should be RECV", "RECV", labels.get("operation"));
        assertEquals("Filename should be data.csv", "data.csv", labels.get("filename"));
    }

    @Test
    public void testMetricsKeyBuilding() {
        Map<String, String> labels = httpPattern.match("POST /api/orders 201");
        String key = httpPattern.buildMetricKey(labels);
        
        assertTrue("Key should contain metric name", key.contains("http_requests_total"));
        assertTrue("Key should contain method label", key.contains("method=\"POST\""));
        assertTrue("Key should contain status label", key.contains("status=\"201\""));
    }

    @Test
    public void testRegistryRecordLogLine() {
        registry.addPattern(httpPattern);
        registry.addPattern(ftpPattern);
        
        // Record HTTP logs
        registry.recordLogLine("GET /api/users 200");
        registry.recordLogLine("GET /api/users 200");  // Duplicate
        registry.recordLogLine("POST /api/orders 201");
        
        // Record FTP logs
        registry.recordLogLine("RECV data.csv");
        registry.recordLogLine("SEND backup.zip");
        
        assertEquals("Should have recorded metrics", 4, registry.getMetricsCount());
    }

    @Test
    public void testMetricsAggregation() {
        registry.addPattern(httpPattern);
        
        // Log multiple GET requests
        registry.recordLogLine("GET /api/users 200");
        registry.recordLogLine("GET /api/users 200");
        registry.recordLogLine("GET /api/users 200");
        
        Map<String, Long> metrics = registry.getMetrics();
        long getCount = 0;
        for (Map.Entry<String, Long> entry : metrics.entrySet()) {
            if (entry.getKey().contains("GET") && entry.getKey().contains("/api/users")) {
                getCount = entry.getValue();
            }
        }
        
        assertEquals("Should have counted 3 GET requests", 3L, getCount);
    }

    @Test
    public void testRegistryReset() {
        registry.addPattern(httpPattern);
        registry.recordLogLine("GET /api/users 200");
        assertEquals("Should have 1 metric", 1, registry.getMetricsCount());
        
        registry.reset();
        assertEquals("Should have 0 metrics after reset", 0, registry.getMetricsCount());
    }

    @Test
    public void testMetricsFileWriter() throws IOException {
        String tempFile = "target/test_metrics.txt";
        new File("target").mkdirs();
        
        registry.addPattern(httpPattern);
        registry.recordLogLine("GET /api/users 200");
        registry.recordLogLine("GET /api/users 200");
        
        MetricsFileWriter writer = new MetricsFileWriter(tempFile, registry);
        writer.flush();
        
        assertTrue("Metrics file should exist", new File(tempFile).exists());
        
        String content = new String(Files.readAllBytes(Paths.get(tempFile)));
        assertTrue("File should contain metric name", content.contains("http_requests_total"));
        assertTrue("File should contain label", content.contains("method=\"GET\""));
        assertTrue("File should contain count", content.contains(" 2"));
        
        // Cleanup
        new File(tempFile).delete();
    }

    @Test
    public void testMetricsPatternParsing() {
        // Test pattern definition string parsing
        String patternDef = "http_get|GET /([\\w/]+)|http_requests_total|endpoint";
        
        String[] parts = patternDef.split("\\|");
        assertEquals("Should have 4 parts", 4, parts.length);
        assertEquals("Name should be http_get", "http_get", parts[0]);
        assertEquals("Regex should be extracted", "GET /([\\w/]+)", parts[1]);
        assertEquals("Metric name should be extracted", "http_requests_total", parts[2]);
        assertEquals("Fields should be extracted", "endpoint", parts[3]);
    }

    @Test
    public void testCardinalityLimit() {
        // Test that cardinality limits prevent memory explosion
        int maxCardinality = 5;
        MetricsRegistry limitedRegistry = new MetricsRegistry(maxCardinality);
        limitedRegistry.addPattern(httpPattern);
        
        // Try to record more unique combinations than the limit
        limitedRegistry.recordLogLine("GET /api/users 200");
        limitedRegistry.recordLogLine("GET /api/orders 200");
        limitedRegistry.recordLogLine("POST /api/orders 201");
        limitedRegistry.recordLogLine("DELETE /api/users 204");
        limitedRegistry.recordLogLine("GET /api/products 200");
        limitedRegistry.recordLogLine("PUT /api/products 200");  // This should be dropped
        
        // Should not exceed cardinality limit
        assertTrue("Metrics count should not exceed cardinality limit", 
                limitedRegistry.getMetricsCount() <= maxCardinality);
    }

    @Test
    public void testMultiplePatternsMultipleMatches() {
        registry.addPattern(httpPattern);
        registry.addPattern(ftpPattern);
        
        // Log that could match multiple patterns
        String logLine = "GET /api/users 200";
        registry.recordLogLine(logLine);
        
        // Should match HTTP pattern
        assertTrue("Should have recorded metric for HTTP pattern", registry.getMetricsCount() >= 1);
    }

    @Test
    public void testMetricsFlushManager() throws InterruptedException {
        String tempFile = "target/test_flush_metrics.txt";
        new File("target").mkdirs();
        
        registry.addPattern(httpPattern);
        registry.recordLogLine("GET /api/users 200");
        
        MetricsFileWriter writer = new MetricsFileWriter(tempFile, registry);
        MetricsFlushManager manager = new MetricsFlushManager(registry, writer, 500);  // 500ms flush interval
        manager.start();
        
        // Wait for flush
        Thread.sleep(1000);
        
        assertTrue("Flush manager should be running", manager.isRunning());
        assertTrue("Metrics file should exist after flush", new File(tempFile).exists());
        
        manager.stop();
        assertFalse("Flush manager should be stopped", manager.isRunning());
        
        // Cleanup
        new File(tempFile).delete();
    }

    @Test
    public void testPrometheusFormat() {
        registry.addPattern(httpPattern);
        registry.recordLogLine("GET /api/users 200");
        
        Map<String, Long> metrics = registry.getMetrics();
        
        // Verify Prometheus format
        for (String key : metrics.keySet()) {
            assertTrue("Key should start with metric name", key.startsWith("http_requests_total"));
            assertTrue("Key should have labels in curly braces", key.contains("{") && key.contains("}"));
            assertTrue("Key should have labeled values", key.contains("=\""));
        }
    }
}
