package com.logguardai.ai;

/**
 * Exception thrown when AI service calls fail.
 */
public class AIServiceException extends Exception {
    
    public AIServiceException(String message) {
        super(message);
    }
    
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public AIServiceException(String message, long timeoutMs) {
        super(String.format("%s (timeout after %dms)", message, timeoutMs));
    }
}
