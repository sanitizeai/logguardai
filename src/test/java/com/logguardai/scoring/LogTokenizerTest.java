package com.logguardai.scoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

import com.logguardai.model.Token;
import com.logguardai.tokenizer.LogTokenizer;

public class LogTokenizerTest {
    
    private LogTokenizer tokenizer = new LogTokenizer();
    
    @Test
    public void testTokenizeKeyValuePairs() {
        String message = "userId=123456789 token=abc123xyz status=active";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(3, tokens.size());
        assertEquals("userId", tokens.get(0).getKey());
        assertEquals("123456789", tokens.get(0).getValue());
    }
    
    @Test
    public void testTokenizeEmptyString() {
        String message = "";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }
    
    @Test
    public void testTokenizeNull() {
        List<Token> tokens = tokenizer.tokenize(null);
        
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }
    
    @Test
    public void testTokenizeNoKeyValuePairs() {
        String message = "This is a simple log message without structured data";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }
    
    @Test
    public void testTokenizeJSON() {
        String message = "{\"userId\":\"123\",\"token\":\"abc123\"}";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
    }
    
    @Test
    public void testTokenizeWithPunctuation() {
        String message = "userId=123456789, token=abc123xyz;";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("abc123xyz", tokens.get(1).getValue());
    }

    @Test
    public void testTokenizePasswordAndApiKey() {
        String message = "Test log: password=secret123 apiKey=sk-abc123";
        List<Token> tokens = tokenizer.tokenize(message);

        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        assertEquals("password", tokens.get(0).getKey());
        assertEquals("secret123", tokens.get(0).getValue());
        assertEquals("apiKey", tokens.get(1).getKey());
        assertEquals("sk-abc123", tokens.get(1).getValue());
    }

    @Test
    public void testTokenizeBracketedWithUUIDs() {
        // Test the actual Spring Boot log format with bracketed key-value pairs
        String message = "[requestId=ebccc99c-aa60-47a0-9c76-369c36251e79, correlationId=9f4ba3b1-ee81-40a0-8084-a3b822baaa6b]";
        List<Token> tokens = tokenizer.tokenize(message);
        
        assertNotNull(tokens);
        assertEquals(2, tokens.size());
        
        // Verify first token
        assertEquals("requestId", tokens.get(0).getKey());
        assertEquals("ebccc99c-aa60-47a0-9c76-369c36251e79", tokens.get(0).getValue());
        
        // Verify second token - should NOT include the closing bracket
        assertEquals("correlationId", tokens.get(1).getKey());
        assertEquals("9f4ba3b1-ee81-40a0-8084-a3b822baaa6b", tokens.get(1).getValue());
    }
}
