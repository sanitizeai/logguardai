package com.logguardai.scoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.logguardai.model.Token;

/**
 * Risk Scoring Engine that assigns a risk score to tokens based on heuristics.
 * 
 * Scoring Logic:
 * - Sensitive keywords in key: +2-3
 * - Value characteristics (length, entropy, patterns): +1-3
 * - Safe key patterns: 0 (whitelisted keys, e.g., correlationId, traceId)
 * - UUID values: 0 (standard UUIDs are typically identifiers, not secrets)
 * 
 * Total Score:
 * 0-2: Low risk (no action)
 * 3-5: Medium risk (rule-based masking)
 * >5: High risk (AI sanitization if enabled)
 */
public class RiskScoringEngine {
    
    // High-risk sensitive keywords that should strongly increase risk.
    private static final Set<String> HIGH_RISK_KEYWORDS = new HashSet<>(Arrays.asList(
        "password", "passwd", "pwd", "secret", "apikey", "api_key",
        "token", "bearer", "authorization", "credential", "credentials"
    ));

    // Sensitive keyword variations
    private static final Set<String> SENSITIVE_KEYWORDS = new HashSet<>(Arrays.asList(
        "id", "token", "password", "passwd", "pwd", "secret", "auth",
        "apikey", "api_key", "key", "credential", "credentials",
        "bearer", "authorization", "session", "sessionid", "jwt",
        "pii", "ssn", "social", "license", "creditcard", "card",
        "private", "confidential", "encrypted", "hash"
    ));

    // Patterns for detecting sensitive data
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{20,}$");
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]+=*$");
    private static final Pattern NUMERIC_ONLY_PATTERN = Pattern.compile("^\\d{8,}$");
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{16,}$");
    // RFC 4122 UUID format: 550e8400-e29b-41d4-a716-446655440000
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    
    // Safe key patterns (compiled from user config or defaults)
    private final List<Pattern> safeKeyPatterns;

    /**
     * Default constructor with no safe key patterns.
     */
    public RiskScoringEngine() {
        this.safeKeyPatterns = new ArrayList<>();
    }

    /**
     * Constructor with custom safe key patterns.
     * @param safeKeyPatterns List of regex patterns for safe keys (e.g., ".*correlation.*", "x-.*-id")
     */
    public RiskScoringEngine(List<String> safeKeyPatterns) {
        this.safeKeyPatterns = new ArrayList<>();
        if (safeKeyPatterns != null) {
            for (String pattern : safeKeyPatterns) {
                try {
                    this.safeKeyPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                } catch (Exception e) {
                    // Log warning but don't fail
                    System.err.println("Warning: Invalid safe key pattern '" + pattern + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Score a single token and update its risk score.
     * Short-circuits for UUIDs (always safe) and safe key patterns.
     */
    public void scoreToken(Token token) {
        String value = token.getValue();
        String key = token.getKey();
        
        // UUID values are always safe (intrinsically safe identifiers), regardless of key
        if (isUUID(value)) {
            token.setRiskScore(0);
            return;
        }
        
        // Check if key matches any safe pattern - if so, entire token is safe
        if (key != null && !key.isEmpty()) {
            String lowerKey = key.toLowerCase();
            for (Pattern safePattern : safeKeyPatterns) {
                if (safePattern.matcher(lowerKey).matches()) {
                    token.setRiskScore(0);
                    return;  // Safe key pattern, entire token is safe
                }
            }
        }
        
        int keyScore = scoreKey(key);
        int valueScore = scoreValue(value);
        int totalScore = keyScore + valueScore;
        token.setRiskScore(totalScore);
    }

    /**
     * Score multiple tokens.
     */
    public void scoreTokens(List<Token> tokens) {
        for (Token token : tokens) {
            scoreToken(token);
        }
    }

    /**
     * Score the key part of a token.
     * Returns 0 if key matches a safe pattern (e.g., correlationId, traceId).
     */
    private int scoreKey(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        String lowerKey = key.toLowerCase();
        
        // Check if key matches any safe pattern first
        for (Pattern safePattern : safeKeyPatterns) {
            if (safePattern.matcher(lowerKey).matches()) {
                return 0;  // Safe key, no risk
            }
        }
        
        // High-risk keys should score more aggressively for shorter secret values.
        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (lowerKey.contains(keyword)) {
                return 3;
            }
        }

        int score = 0;

        // Check for sensitive keywords
        for (String keyword : SENSITIVE_KEYWORDS) {
            if (lowerKey.contains(keyword)) {
                score += 2;
                break;
            }
        }

        return score;
    }

    /**
     * Score the value part of a token based on characteristics.
     * Returns 0 immediately if value is a standard UUID (safe identifier).
     */
    private int scoreValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }

        // UUID format (RFC 4122) is intrinsically safe - it's a standard identifier
        if (isUUID(value)) {
            return 0;  // UUIDs are safe by default
        }

        int score = 0;

        // Length check: longer random strings are more likely to be secrets
        if (value.length() > 12) {
            score += 1;
        }

        // Entropy check: random-looking strings
        if (hasHighEntropy(value)) {
            score += 2;
        }

        // Numeric sequences (IDs)
        if (NUMERIC_ONLY_PATTERN.matcher(value).matches()) {
            score += 1;
        }

        // Hex patterns (tokens, hashes)
        if (HEX_PATTERN.matcher(value).matches()) {
            score += 2;
        }

        // JWT-like pattern
        if (JWT_PATTERN.matcher(value).matches()) {
            score += 2;
        }

        // Base64 pattern
        if (value.length() > 20 && BASE64_PATTERN.matcher(value).matches()) {
            score += 1;
        }

        return score;
    }

    /**
     * Detect if value is a standard UUID (RFC 4122 format).
     * Format: 550e8400-e29b-41d4-a716-446655440000
     */
    private boolean isUUID(String value) {
        return UUID_PATTERN.matcher(value).matches();
    }

    /**
     * Detect high entropy strings (random-looking).
     * Simple heuristic: ratio of unique characters to total length.
     */
    private boolean hasHighEntropy(String value) {
        if (value.length() < 10) {
            return false;
        }

        Set<Character> uniqueChars = new HashSet<>();
        for (char c : value.toCharArray()) {
            uniqueChars.add(c);
        }

        // If more than 50% unique characters, likely high entropy
        double entropy = (double) uniqueChars.size() / value.length();
        return entropy > 0.5;
    }
}
