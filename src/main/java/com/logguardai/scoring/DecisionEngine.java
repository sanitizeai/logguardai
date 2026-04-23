package com.logguardai.scoring;

/**
 * Decision Engine that determines the action based on risk score.
 * 
 * Decision Logic:
 * 0-2: No action (pass through)
 * 3-5: Rule-based masking
 * >5: AI sanitization (if enabled)
 */
public class DecisionEngine {
    
    public static final int LOW_RISK_THRESHOLD = 2;
    public static final int MEDIUM_RISK_THRESHOLD = 5;
    
    public enum SanitizationAction {
        PASS_THROUGH,      // No masking
        RULE_BASED_MASK,   // Apply rule-based masking
        AI_SANITIZE        // Send to AI for sanitization
    }

    /**
     * Determine the action based on risk score.
     */
    public SanitizationAction decideSanitization(int riskScore) {
        if (riskScore <= LOW_RISK_THRESHOLD) {
            return SanitizationAction.PASS_THROUGH;
        } else if (riskScore <= MEDIUM_RISK_THRESHOLD) {
            return SanitizationAction.RULE_BASED_MASK;
        } else {
            return SanitizationAction.AI_SANITIZE;
        }
    }

    /**
     * Check if AI sanitization should be considered.
     */
    public boolean shouldConsiderAI(int riskScore) {
        return riskScore > MEDIUM_RISK_THRESHOLD;
    }
}
