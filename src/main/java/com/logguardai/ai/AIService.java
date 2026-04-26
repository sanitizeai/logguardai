package com.logguardai.ai;

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
     * Classify a value as PII or determine its sensitivity level.
     * 
     * @param value The value to classify
     * @param context Field name or context
     * @return Classification: "pii", "sensitive", "public", or "unknown"
     * @throws AIServiceException if call fails
     */
    String classifyData(String value, String context) throws AIServiceException;
    
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
