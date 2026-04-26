package com.logguardai.client;

import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;

/**
 * No-op AIService stub implementation.
 * Used when AI is disabled or not properly configured.
 * Always returns safe default responses without making API calls.
 */
public class NoOpAIService implements AIService {
    
    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        return "[REDACTED]";
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet) 
            throws AIServiceException {
        // Map to known exceptions
        if (exceptionType.contains("NullPointerException")) {
            return "Null reference accessed. Ensure objects are initialized before use.";
        } else if (exceptionType.contains("IllegalArgumentException")) {
            return "Invalid argument passed. Check input validation and expected ranges.";
        } else if (exceptionType.contains("IndexOutOfBoundsException")) {
            return "Index out of bounds. Validate array/collection indices before access.";
        }
        return "An error occurred. Check the exception message and stack trace for details.";
    }

    @Override
    public String classifyData(String value, String context) throws AIServiceException {
        if (value == null || value.isEmpty()) {
            return "unknown";
        }
        
        String lower = value.toLowerCase();
        if (lower.matches(".*[a-f0-9]{32,}.*") || lower.matches(".*[a-z0-9+/=]{40,}.*")) {
            return "sensitive";
        }
        return "public";
    }

    @Override
    public boolean isHealthy() {
        return true;  // No-op is always "healthy"
    }

    @Override
    public String getServiceName() {
        return "No-Op AI Service (disabled)";
    }
}
