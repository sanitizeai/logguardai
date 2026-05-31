package com.logguardai.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class NoOpAIServiceTest {
    
    private NoOpAIService service = new NoOpAIService();
    
    @Test
    public void testSanitize() throws Exception {
        String result = service.sanitize("secret_value", "password");
        assertNotNull(result);
        assertEquals("[REDACTED]", result);
    }
    
    @Test
    public void testExplainNullPointer() throws Exception {
        String result = service.explainException("java.lang.NullPointerException", "null", "");
        assertNotNull(result);
        assertTrue(result.contains("Null reference"));
    }
    
    @Test
    public void testExplainIllegalArgument() throws Exception {
        String result = service.explainException("java.lang.IllegalArgumentException", "invalid", "");
        assertNotNull(result);
        assertTrue(result.contains("Invalid argument"));
    }
    
    @Test
    public void testClassifyData() throws Exception {
        String result = service.classifyData("user@example.com", "email");
        assertNotNull(result);
        assertTrue(result.equals("public") || result.equals("sensitive"));
    }
    
    @Test
    public void testIsHealthy() {
        assertTrue(service.isHealthy());
    }
    
    @Test
    public void testGetServiceName() {
        String name = service.getServiceName();
        assertNotNull(name);
        assertTrue(name.contains("No-Op"));
    }

    @Test
    public void testSanitizeAsync() throws Exception {
        CompletableFuture<String> future = service.sanitizeAsync("secret_value", "password");
        String result = future.get(200, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertEquals("[REDACTED]", result);
    }
    
    @Test
    public void testSanitizeBatch() throws Exception {
        List<String> values = Arrays.asList("secret1", "secret2", "secret3");
        List<String> contexts = Arrays.asList("password", "token", "key");
        
        Map<String, String> result = service.sanitizeBatch(values, contexts);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("[REDACTED]", result.get("secret1"));
        assertEquals("[REDACTED]", result.get("secret2"));
        assertEquals("[REDACTED]", result.get("secret3"));
    }
    
    @Test
    public void testSanitizeBatchEmpty() throws Exception {
        List<String> values = new ArrayList<>();
        List<String> contexts = new ArrayList<>();
        
        Map<String, String> result = service.sanitizeBatch(values, contexts);
        
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testClassifyDataBatch() throws Exception {
        List<String> values = Arrays.asList("user@example.com", "a1b2c3d4e5f678901234567890123456789012", "normal text");
        List<String> contexts = Arrays.asList("email", "apiKey", "message");
        
        Map<String, String> result = service.classifyDataBatch(values, contexts);
        
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("public", result.get("user@example.com"));
        assertEquals("sensitive", result.get("a1b2c3d4e5f678901234567890123456789012"));
        assertEquals("public", result.get("normal text"));
    }
    
    @Test
    public void testClassifyDataBatchAsync() throws Exception {
        List<String> values = Arrays.asList("abcdef12345678901234567890123456789012", "normal");
        List<String> contexts = Arrays.asList("password", "text");
        
        CompletableFuture<Map<String, String>> future = service.classifyDataBatchAsync(values, contexts);
        Map<String, String> result = future.get(200, TimeUnit.MILLISECONDS);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("sensitive", result.get("abcdef12345678901234567890123456789012"));
        assertEquals("public", result.get("normal"));
    }
}
