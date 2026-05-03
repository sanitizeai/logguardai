package com.logguardai.tokenizer;

import com.google.gson.*;
import com.logguardai.model.Token;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizer for breaking log messages into meaningful key-value pairs.
 * Supports:
 * - key=value pairs (space-separated)
 * - JSON-like logs
 * - Query string format
 */
public class LogTokenizer {
    // Pattern to match key=value pairs, excluding common closing punctuation
    // Matches: key=value where value stops at whitespace, comma, or closing brackets/braces
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("(\\w+)=([^\\s,\\]\\)\\}]+)");
    private static final JsonParser jsonParser = new JsonParser();

    /**
     * Extract tokens from a log message.
     * Attempts JSON parsing first, falls back to key=value pattern matching.
     */
    public List<Token> tokenize(String logMessage) {
        if (logMessage == null || logMessage.isEmpty()) {
            return new ArrayList<>();
        }

        List<Token> tokens = new ArrayList<>();

        // Try JSON parsing first
        try {
            tokens.addAll(parseJson(logMessage));
            if (!tokens.isEmpty()) {
                return tokens;
            }
        } catch (Exception e) {
            // Not JSON, continue with key=value parsing
        }

        // Fallback to key=value pattern matching
        tokens.addAll(parseKeyValuePairs(logMessage));

        return tokens;
    }

    /**
     * Parse JSON log message into tokens.
     */
    @SuppressWarnings("deprecation")
    private List<Token> parseJson(String logMessage) throws JsonSyntaxException {
        List<Token> tokens = new ArrayList<>();
        JsonObject jsonObject = jsonParser.parse(logMessage).getAsJsonObject();

        for (String key : jsonObject.keySet()) {
            JsonElement element = jsonObject.get(key);
            String value = element.isJsonPrimitive() ? element.getAsString() : element.toString();
            tokens.add(new Token(key, value));
        }

        return tokens;
    }

    /**
     * Parse key=value pairs from log message.
     * Examples: "userId=12345 token=abc123 name=john"
     * Also handles bracketed format: "[key1=value1, key2=value2]"
     */
    private List<Token> parseKeyValuePairs(String logMessage) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(logMessage);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            // Remove any remaining trailing punctuation like commas or semicolons
            value = value.replaceAll("[,;:]+$", "");
            tokens.add(new Token(key, value));
        }

        return tokens;
    }
}
