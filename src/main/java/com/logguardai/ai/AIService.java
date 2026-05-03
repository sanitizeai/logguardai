package com.logguardai.ai;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.List;
import java.util.Map;

/**
 * AIService interface for integrating AI-based sanitization, exception explanations,
 * and PII data classification.
 * 
 * Implementations should handle:
 * - Timeout protection (max 1-2 seconds per call)
 * - Fallback to rule-based masking on error
 * - API credential management
 */
public interface AIService {
    
    /**
     * Sanitize a high-risk value using AI analysis.
     * Returns a more sophisticated sanitization than simple masking.
     * 
     * @param value The sensitive value to sanitize
     * @param context Optional context (e.g., field name: "password", "token")
     * @return Sanitized/anonymized version of the value
     * @throws AIServiceException if call fails after timeout or API error
     */
    String sanitize(String value, String context) throws AIServiceException;

    /**
     * Asynchronously sanitize a high-risk value using AI analysis.
     * Implementations may choose a non-blocking backend or wrap the sync call.
     */
    default CompletableFuture<String> sanitizeAsync(String value, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sanitize(value, context);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Generate AI-enhanced explanation for an exception.
     * 
     * @param exceptionType Fully qualified exception class name
     * @param message Exception message
     * @param stackTraceSnippet First few lines of stack trace
     * @return Human-friendly explanation with cause and fix suggestions
     * @throws AIServiceException if call fails
     */
    String explainException(String exceptionType, String message, String stackTraceSnippet) 
            throws AIServiceException;

    /**
     * Asynchronously explain an exception.
     */
    default CompletableFuture<String> explainExceptionAsync(String exceptionType, String message, String stackTraceSnippet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return explainException(exceptionType, message, stackTraceSnippet);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Classify a value as PII or determine its sensitivity level.
     * 
     * @param value The value to classify
     * @param context Field name or context
     * @return Classification: "pii", "sensitive", "public", or "unknown"
     * @throws AIServiceException if call fails
     */
    String classifyData(String value, String context) throws AIServiceException;

    /**
     * Asynchronously classify a value.
     */
    default CompletableFuture<String> classifyDataAsync(String value, String context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return classifyData(value, context);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Batch sanitize multiple values using AI analysis.
     * Returns a map of original values to their sanitized versions.
     * 
     * @param values List of sensitive values to sanitize
     * @param contexts List of contexts (field names) corresponding to values
     * @return Map of original value to sanitized value
     * @throws AIServiceException if call fails
     */
    Map<String, String> sanitizeBatch(List<String> values, List<String> contexts) throws AIServiceException;

    /**
     * Asynchronously batch sanitize multiple values.
     */
    default CompletableFuture<Map<String, String>> sanitizeBatchAsync(List<String> values, List<String> contexts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sanitizeBatch(values, contexts);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Batch classify multiple values as PII or determine their sensitivity levels.
     * 
     * @param values List of values to classify
     * @param contexts List of field names or contexts corresponding to values
     * @return Map of value to classification ("pii", "sensitive", "public", "unknown")
     * @throws AIServiceException if call fails
     */
    Map<String, String> classifyDataBatch(List<String> values, List<String> contexts) throws AIServiceException;

    /**
     * Asynchronously batch classify multiple values.
     */
    default CompletableFuture<Map<String, String>> classifyDataBatchAsync(List<String> values, List<String> contexts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return classifyDataBatch(values, contexts);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    /**
     * Check if the AI service is healthy and accessible.
     * 
     * @return true if service is initialized and responding
     */
    boolean isHealthy();
    
    /**
     * Get the name of this AI service implementation.
     * 
     * @return Service name (e.g., "OpenAI GPT-3.5")
     */
    String getServiceName();
}
