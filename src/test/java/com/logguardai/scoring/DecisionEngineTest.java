package com.logguardai.scoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DecisionEngineTest {
    
    private DecisionEngine decisionEngine = new DecisionEngine();
    
    @Test
    public void testDecideLowRisk() {
        DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(1);
        assertEquals(DecisionEngine.SanitizationAction.PASS_THROUGH, action);
    }
    
    @Test
    public void testDecideMediumRisk() {
        DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(4);
        assertEquals(DecisionEngine.SanitizationAction.RULE_BASED_MASK, action);
    }
    
    @Test
    public void testDecideHighRisk() {
        DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(6);
        assertEquals(DecisionEngine.SanitizationAction.AI_SANITIZE, action);
    }
    
    @Test
    public void testShouldConsiderAI() {
        assertFalse(decisionEngine.shouldConsiderAI(3));
        assertTrue(decisionEngine.shouldConsiderAI(6));
    }
    
    @Test
    public void testBoundaryLowToMedium() {
        DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(2);
        assertEquals(DecisionEngine.SanitizationAction.PASS_THROUGH, action);
        
        action = decisionEngine.decideSanitization(3);
        assertEquals(DecisionEngine.SanitizationAction.RULE_BASED_MASK, action);
    }
    
    @Test
    public void testBoundaryMediumToHigh() {
        DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(5);
        assertEquals(DecisionEngine.SanitizationAction.RULE_BASED_MASK, action);
        
        action = decisionEngine.decideSanitization(6);
        assertEquals(DecisionEngine.SanitizationAction.AI_SANITIZE, action);
    }
}
