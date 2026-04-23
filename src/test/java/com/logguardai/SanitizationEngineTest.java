package com.logguardai.sanitizer;

import org.junit.Test;
import static org.junit.Assert.*;

public class SanitizationEngineTest {
    
    private SanitizationEngine sanitizationEngine = new SanitizationEngine();
    
    @Test
    public void testMaskValue() {
        String result = sanitizationEngine.maskValue("secret123");
        assertEquals("*****", result);
    }
    
    @Test
    public void testMaskNull() {
        String result = sanitizationEngine.maskValue(null);
        assertEquals("*****", result);
    }
    
    @Test
    public void testBuildMaskedPair() {
        String result = sanitizationEngine.buildMaskedPair("password", "mySecret");
        assertEquals("password=*****", result);
    }
    
    @Test
    public void testMaskLogMessage() {
        String original = "userId=123456 token=abc123xyz";
        java.util.List<String> valuesToMask = new java.util.ArrayList<>();
        valuesToMask.add("123456");
        valuesToMask.add("abc123xyz");
        
        String result = sanitizationEngine.maskLogMessage(original, valuesToMask);
        assertTrue(result.contains("*****"));
        assertFalse(result.contains("123456"));
        assertFalse(result.contains("abc123xyz"));
    }
}
