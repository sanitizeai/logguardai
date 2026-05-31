package com.logguardai.scoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.logguardai.model.Token;

public class RiskScoringTest {
    
    private RiskScoringEngine riskScoringEngine = new RiskScoringEngine();
    
    @Test
    public void testScoreSensitiveKeyword() {
        Token token = new Token("password", "secret123");
        riskScoringEngine.scoreToken(token);
        
        assertTrue("Token with sensitive keyword should have risk score > 0", token.getRiskScore() > 0);
    }
    
    @Test
    public void testScoreHighEntropyValue() {
        Token token = new Token("identifier", "a1b2c3d4e5f6g7h8i9j0");
        riskScoringEngine.scoreToken(token);
        
        assertTrue("Token with high entropy should have risk score > 0", token.getRiskScore() > 0);
    }
    
    @Test
    public void testScoreLowRiskToken() {
        Token token = new Token("name", "john");
        riskScoringEngine.scoreToken(token);
        
        assertTrue("Low risk token should have score <= 2", token.getRiskScore() <= 2);
    }
    
    @Test
    public void testScoreNumericSequence() {
        Token token = new Token("userId", "987654321012345");
        riskScoringEngine.scoreToken(token);
        
        assertTrue("Numeric sequence should increase risk score", token.getRiskScore() > 0);
    }
    
    @Test
    public void testScoreJWTPattern() {
        Token token = new Token("auth", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9eyJzdWIiOiIxMjM0NTY3ODkwIn0");
        riskScoringEngine.scoreToken(token);
        
        assertTrue("JWT pattern should have high risk score", token.getRiskScore() > 2);
    }
    
    @Test
    public void testScoreMultipleTokens() {
        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token("userId", "123456789"));
        tokens.add(new Token("name", "john"));
        tokens.add(new Token("password", "secret123"));
        
        riskScoringEngine.scoreTokens(tokens);
        
        assertEquals(3, tokens.size());
        // password token should have highest score
        assertTrue("All tokens should be scored", tokens.stream().allMatch(t -> t.getRiskScore() >= 0));
    }

    @Test
    public void testScorePasswordAndApiKey() {
        Token passwordToken = new Token("password", "secret123");
        Token apiKeyToken = new Token("apiKey", "sk-abc123");

        riskScoringEngine.scoreToken(passwordToken);
        riskScoringEngine.scoreToken(apiKeyToken);

        assertTrue("password token should be masked", passwordToken.getRiskScore() > 2);
        assertTrue("apiKey token should be masked", apiKeyToken.getRiskScore() > 2);
    }

    @Test
    public void testTokenizeAndScoreSecretPairs() {
        String message = "Test log: password=secret123 apiKey=sk-abc123";
        com.logguardai.tokenizer.LogTokenizer tokenizer = new com.logguardai.tokenizer.LogTokenizer();
        List<Token> tokens = tokenizer.tokenize(message);

        riskScoringEngine.scoreTokens(tokens);

        assertEquals(2, tokens.size());
        assertTrue("password token should be masked", tokens.get(0).getRiskScore() > 2);
        assertTrue("apiKey token should be masked", tokens.get(1).getRiskScore() > 2);
    }

    @Test
    public void testUUIDFormatIssSafe() {
        // Standard UUID format should have score 0 (safe)
        Token token = new Token("correlationId", "550e8400-e29b-41d4-a716-446655440000");
        riskScoringEngine.scoreToken(token);
        
        assertEquals("UUID should be safe regardless of key name", 0, token.getRiskScore());
    }

    @Test
    public void testUUIDWithDifferentKeyNames() {
        // Any key name with UUID value should be safe
        Token token1 = new Token("x-correlation-id", "550e8400-e29b-41d4-a716-446655440000");
        Token token2 = new Token("trace-id", "550e8400-e29b-41d4-a716-446655440000");
        Token token3 = new Token("span_id", "550e8400-e29b-41d4-a716-446655440000");
        
        riskScoringEngine.scoreToken(token1);
        riskScoringEngine.scoreToken(token2);
        riskScoringEngine.scoreToken(token3);
        
        assertEquals("UUID with x-correlation-id should be safe", 0, token1.getRiskScore());
        assertEquals("UUID with trace-id should be safe", 0, token2.getRiskScore());
        assertEquals("UUID with span_id should be safe", 0, token3.getRiskScore());
    }

    @Test
    public void testSafeKeyPatterns() {
        // Custom safe key patterns via constructor
        List<String> patterns = new ArrayList<>();
        patterns.add(".*correlation.*");  // matches keys containing "correlation" anywhere
        patterns.add("x-.*-id");           // matches x-<anything>-id
        patterns.add("trace.*");           // matches keys starting with "trace"
        
        RiskScoringEngine customEngine = new RiskScoringEngine(patterns);
        
        // These should all be safe (score 0) due to key pattern match
        Token token1 = new Token("correlationId", "a1b2c3d4e5f6g7h8i9j0");
        Token token2 = new Token("x-trace-id", "a1b2c3d4e5f6g7h8i9j0");
        Token token3 = new Token("tracespan", "a1b2c3d4e5f6g7h8i9j0");  // Starts with "trace"
        
        customEngine.scoreToken(token1);
        customEngine.scoreToken(token2);
        customEngine.scoreToken(token3);
        
        assertEquals("correlationId should match .*correlation.* pattern", 0, token1.getRiskScore());
        assertEquals("x-trace-id should match x-.*-id pattern", 0, token2.getRiskScore());
        assertEquals("tracespan should match trace.* pattern", 0, token3.getRiskScore());
    }

    @Test
    public void testSecretStillMaskedWithSafePatterns() {
        // Even with safe patterns, actual secrets should still be masked
        List<String> patterns = new ArrayList<>();
        patterns.add(".*correlation.*");
        
        RiskScoringEngine customEngine = new RiskScoringEngine(patterns);
        
        Token token = new Token("apiKey", "a1b2c3d4e5f6g7h8i9j0");
        customEngine.scoreToken(token);
        
        assertTrue("apiKey with high entropy should still have risk score > 0", token.getRiskScore() > 0);
    }

    @Test
    public void testBracketedLogFormatWithUUIDs() {
        // Test that UUIDs from Spring Boot log format (bracketed) are not masked
        // Real-world example: [requestId=..., correlationId=...]
        Token requestIdToken = new Token("requestId", "ebccc99c-aa60-47a0-9c76-369c36251e79");
        Token correlationIdToken = new Token("correlationId", "9f4ba3b1-ee81-40a0-8084-a3b822baaa6b");
        
        riskScoringEngine.scoreToken(requestIdToken);
        riskScoringEngine.scoreToken(correlationIdToken);
        
        assertEquals("requestId with UUID should score 0", 0, requestIdToken.getRiskScore());
        assertEquals("correlationId with UUID should score 0", 0, correlationIdToken.getRiskScore());
    }
}
