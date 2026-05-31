package com.logguardai.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception Processor that generates human-friendly insights for exceptions.
 * 
 * v0.1 features:
 * - Maps common exceptions to likely causes
 * - Provides fallback insights
 * - Extracts stack trace for analysis
 * 
 * Future: AI-based exception explanation
 */
public class ExceptionProcessor {
    
    private static final Map<String, String> EXCEPTION_INSIGHTS = new HashMap<>();

    static {
        // NullPointerException
        EXCEPTION_INSIGHTS.put("java.lang.NullPointerException",
            "A null reference was used. " +
            "Cause: object was not initialized before use. " +
            "Action: check object initialization paths.");

        // IllegalArgumentException
        EXCEPTION_INSIGHTS.put("java.lang.IllegalArgumentException",
            "Invalid argument provided. " +
            "Cause: argument value violates contract or validation rules. " +
            "Action: review input validation and expected value ranges.");

        // IndexOutOfBoundsException
        EXCEPTION_INSIGHTS.put("java.lang.IndexOutOfBoundsException",
            "Array or collection index out of bounds. " +
            "Cause: accessing index outside valid range. " +
            "Action: validate index before access or use safe iteration.");

        // NumberFormatException
        EXCEPTION_INSIGHTS.put("java.lang.NumberFormatException",
            "Failed to parse string to number. " +
            "Cause: string contains non-numeric characters. " +
            "Action: validate string format before parsing.");

        // IllegalStateException
        EXCEPTION_INSIGHTS.put("java.lang.IllegalStateException",
            "Object in invalid state for operation. " +
            "Cause: operation called at wrong time or state. " +
            "Action: verify preconditions and object lifecycle.");

        // ClassCastException
        EXCEPTION_INSIGHTS.put("java.lang.ClassCastException",
            "Incompatible class cast. " +
            "Cause: object is not instance of target class. " +
            "Action: check type before casting or use instanceof.");

        // UnsupportedOperationException
        EXCEPTION_INSIGHTS.put("java.lang.UnsupportedOperationException",
            "Operation not supported. " +
            "Cause: method not implemented for this object. " +
            "Action: use a supported operation or alternative implementation.");

        // OutOfMemoryError
        EXCEPTION_INSIGHTS.put("java.lang.OutOfMemoryError",
            "Heap memory exhausted. " +
            "Cause: too many objects or large data structures. " +
            "Action: increase heap size or optimize memory usage.");

        // StackOverflowError
        EXCEPTION_INSIGHTS.put("java.lang.StackOverflowError",
            "Stack overflow from infinite recursion. " +
            "Cause: recursive method without proper termination. " +
            "Action: check recursion termination condition.");

        // TimeoutException
        EXCEPTION_INSIGHTS.put("java.util.concurrent.TimeoutException",
            "Operation timed out. " +
            "Cause: operation took longer than allowed timeout. " +
            "Action: increase timeout or optimize performance.");
    }

    /**
     * Generate an insight string for a throwable.
     */
    public String generateInsight(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        String exceptionClassName = throwable.getClass().getName();
        String message = throwable.getMessage();
        
        // Get predefined insight
        String insight = EXCEPTION_INSIGHTS.getOrDefault(exceptionClassName,
            "Unexpected error occurred. Review exception message and stack trace.");

        // Append exception message if available
        if (message != null && !message.isEmpty()) {
            insight += " Details: " + sanitizeMessage(message);
        }

        return insight;
    }

    /**
     * Extract brief exception info for logging.
     */
    public String getExceptionSummary(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Exception: ").append(throwable.getClass().getSimpleName());
        
        if (throwable.getMessage() != null) {
            summary.append(" - ").append(trimMessage(throwable.getMessage(), 100));
        }

        return summary.toString();
    }

    /**
     * Get stack trace string (limited to first N lines).
     */
    public String getStackTraceSummary(Throwable throwable, int maxLines) {
        if (throwable == null) {
            return "";
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < Math.min(stackTrace.length, maxLines); i++) {
            sb.append(stackTrace[i].toString()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Sanitize exception message (remove potential sensitive data).
     */
    private String sanitizeMessage(String message) {
        // Basic sanitization: truncate if too long
        return trimMessage(message, 200);
    }

    /**
     * Trim message to maximum length.
     */
    private String trimMessage(String message, int maxLength) {
        if (message == null || message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}
