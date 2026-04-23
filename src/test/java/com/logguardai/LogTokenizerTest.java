package com.logguardai.tokenizer;

import com.logguardai.model.Token;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

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
}
