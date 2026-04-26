package com.logguardai.client;

import com.logguardai.ai.AIService;
import org.junit.Test;
import static org.junit.Assert.*;

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
}
