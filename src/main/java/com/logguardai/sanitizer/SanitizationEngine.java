package com.logguardai.sanitizer;

/**
 * Core sanitization engine that applies rule-based masking.
 * 
 * Masking strategy:
 * - Replace values with fixed mask: *****
 * - Preserve key names (do not mask)
 * - Maintain original structure
 */
public class SanitizationEngine {
    
    public static final String MASK = "*****";

    /**
     * Mask a value with the standard mask.
     */
    public String maskValue(String value) {
        if (value == null) {
            return MASK;
        }
        return MASK;
    }

    /**
     * Build masked key=value pair.
     */
    public String buildMaskedPair(String key, String value) {
        return key + "=" + maskValue(value);
    }

    /**
     * Mask multiple values while preserving structure.
     */
    public String maskLogMessage(String originalMessage, java.util.List<String> valuesToMask) {
        String result = originalMessage;
        for (String value : valuesToMask) {
            result = result.replace(value, MASK);
        }
        return result;
    }
}
