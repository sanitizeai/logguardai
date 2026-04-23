package com.logguardai.scoring;

import com.logguardai.model.Token;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Risk Scoring Engine that assigns a risk score to tokens based on heuristics.
 * 
 * Scoring Logic:
 * - Sensitive keywords in key: +2-3
 * - Value characteristics (length, entropy, patterns): +1-3
 * 
 * Total Score:
 * 0-2: Low risk (no action)
 * 3-5: Medium risk (rule-based masking)
 * >5: High risk (AI sanitization if enabled)
 */
public class RiskScoringEngine {
    
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

    /**
     * Score a single token and update its risk score.
     */
    public void scoreToken(Token token) {
        int keyScore = scoreKey(token.getKey());
        int valueScore = scoreValue(token.getValue());
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
     */
    private int scoreKey(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }

        String lowerKey = key.toLowerCase();
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
     */
    private int scoreValue(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
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
