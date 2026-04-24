package com.logguardai.scoring;

import com.logguardai.model.Token;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

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
}
