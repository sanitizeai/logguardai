package com.logguardai.metrics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Writes metrics to an append-only file in Prometheus text format.
 * Each flush appends a snapshot of current metrics with timestamp.
 */
public class MetricsFileWriter {
    
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final String filePath;
    private final MetricsRegistry registry;
    private final boolean prettyPrint;

    public MetricsFileWriter(String filePath, MetricsRegistry registry) {
        this(filePath, registry, true);
    }

    public MetricsFileWriter(String filePath, MetricsRegistry registry, boolean prettyPrint) {
        this.filePath = filePath;
        this.registry = registry;
        this.prettyPrint = prettyPrint;
        
        // Ensure directory exists
        ensureDirectoryExists();
        // Write header on first run
        writeHeaderIfNew();
    }

    /**
     * Ensure the output directory exists.
     */
    private void ensureDirectoryExists() {
        try {
            File file = new File(filePath);
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            System.err.println("LogGuardAI: Failed to create metrics directory: " + e.getMessage());
        }
    }

    /**
     * Write header if file is new/empty.
     */
    private void writeHeaderIfNew() {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                String header = "# HELP logguardai_metrics LogGuardAI pattern-based metrics\n" +
                        "# TYPE logguardai_metrics counter\n";
                Files.write(Paths.get(filePath), header.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (Exception e) {
            System.err.println("LogGuardAI: Failed to write metrics header: " + e.getMessage());
        }
    }

    /**
     * Flush metrics to file in append-only mode.
     * Format:
     *   # Timestamp: 2026-05-27T10:15:30Z
     *   metric_name{label="value"} count
     *   metric_name{label="value"} count
     *   
     */
    public void flush() {
        try {
            Map<String, Long> metrics = registry.getMetrics();
            if (metrics.isEmpty()) {
                return;  // Nothing to flush
            }

            StringBuilder content = new StringBuilder();
            
            // Add timestamp header
            content.append("\n# Timestamp: ").append(TIMESTAMP_FORMAT.format(new Date())).append("\n");
            
            // Add metrics in sorted order
            metrics.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        content.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
                    });

            // Append to file
            Files.write(Paths.get(filePath), content.toString().getBytes(), 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            registry.updateFlushTime();

        } catch (Exception e) {
            System.err.println("LogGuardAI: Failed to flush metrics to file: " + e.getMessage());
        }
    }

    /**
     * Flush metrics and optionally reset counters.
     */
    public void flushAndReset() {
        flush();
        registry.reset();
    }

    /**
     * Flush and clear only a specific pattern's metrics.
     */
    public void flushPatternAndReset(String patternName) {
        flush();
        registry.resetPattern(patternName);
    }

    /**
     * Get the path to the metrics file.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Get file size in bytes.
     */
    public long getFileSize() {
        try {
            File file = new File(filePath);
            return file.exists() ? file.length() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Clear/rotate the metrics file.
     */
    public void clear() {
        try {
            Files.write(Paths.get(filePath), "".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            writeHeaderIfNew();
        } catch (Exception e) {
            System.err.println("LogGuardAI: Failed to clear metrics file: " + e.getMessage());
        }
    }
}
