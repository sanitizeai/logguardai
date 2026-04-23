package com.logguardai.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Example application demonstrating LogGuardAI usage.
 * 
 * Run with: mvn compile exec:java -Dexec.mainClass="com.logguardai.example.ExampleApp"
 */
public class ExampleApp {
    private static final Logger logger = LogManager.getLogger(ExampleApp.class);

    public static void main(String[] args) {
        logger.info("=== LogGuardAI v0.1 Example ===");
        
        // Example 1: User authentication log with sensitive data
        logger.info("Example 1: Authentication log");
        String userId = "839274923749237";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9eyJzdWIiOiIxMjM0NTY3ODkwIn0";
        String apiKey = "sk-abcdef123456789xyz";
        logger.info("User login: userId={} token={} apiKey={}", userId, token, apiKey);
        logger.info("Expected: User login: userId=***** token=***** apiKey=*****");
        
        // Example 2: JSON-structured log
        logger.info("\nExample 2: JSON-structured log");
        String jsonLog = "{\"userId\":\"123456789\",\"action\":\"payment\",\"amount\":99.99,\"creditCard\":\"4532123456789012\"}";
        logger.info("Transaction: " + jsonLog);
        
        // Example 3: Normal log with non-sensitive data
        logger.info("\nExample 3: Normal log (no masking)");
        logger.info("User activity: username=john_doe email=john@example.com status=active");
        
        // Example 4: Exception with insights
        logger.info("\nExample 4: Exception handling");
        try {
            Object obj = null;
            obj.toString();
        } catch (NullPointerException e) {
            logger.error("Operation failed with exception", e);
            logger.info("Expected: Exception insight about null reference");
        }
        
        // Example 5: Query string format
        logger.info("\nExample 5: Query string format");
        logger.info("API request: sessionId=abc123def456 requestId=req-987654 secret=topsecret123456");
        
        // Example 6: Multiple IDs and tokens
        logger.info("\nExample 6: Multiple sensitive fields");
        logger.info("Batch operation: jobId=12345678901234 token1=xyz789abc123 token2=qwe456rty789 password=MyPassword123");
        
        logger.info("\n=== Examples Complete ===");
    }
}
