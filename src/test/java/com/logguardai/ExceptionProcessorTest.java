package com.logguardai.exception;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExceptionProcessorTest {
    
    private ExceptionProcessor exceptionProcessor = new ExceptionProcessor();
    
    @Test
    public void testGenerateInsightNullPointerException() {
        NullPointerException e = new NullPointerException("test message");
        String insight = exceptionProcessor.generateInsight(e);
        
        assertNotNull(insight);
        assertTrue(insight.contains("null reference"));
        assertTrue(insight.contains("test message"));
    }
    
    @Test
    public void testGenerateInsightIllegalArgumentException() {
        IllegalArgumentException e = new IllegalArgumentException("invalid value");
        String insight = exceptionProcessor.generateInsight(e);
        
        assertNotNull(insight);
        assertTrue(insight.contains("Invalid argument"));
    }
    
    @Test
    public void testGenerateInsightNumberFormatException() {
        NumberFormatException e = new NumberFormatException("not a number");
        String insight = exceptionProcessor.generateInsight(e);
        
        assertNotNull(insight);
        assertTrue(insight.contains("number"));
    }
    
    @Test
    public void testGetExceptionSummaryNull() {
        String summary = exceptionProcessor.getExceptionSummary(null);
        assertEquals("", summary);
    }
    
    @Test
    public void testGetExceptionSummary() {
        Exception e = new Exception("test error");
        String summary = exceptionProcessor.getExceptionSummary(e);
        
        assertNotNull(summary);
        assertTrue(summary.contains("Exception"));
        assertTrue(summary.contains("test error"));
    }
    
    @Test
    public void testGetStackTraceSummary() {
        Exception e = new Exception("test");
        String stackTrace = exceptionProcessor.getStackTraceSummary(e, 3);
        
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length() > 0);
    }
    
    @Test
    public void testGetStackTraceSummaryNull() {
        String stackTrace = exceptionProcessor.getStackTraceSummary(null, 3);
        assertEquals("", stackTrace);
    }
}
