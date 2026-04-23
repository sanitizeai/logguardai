package com.logguardai.model;

/**
 * Represents a token extracted from a log message.
 * A token consists of a key-value pair identified from structured or semi-structured logs.
 */
public class Token {
    private String key;
    private String value;
    private int riskScore;

    public Token(String key, String value) {
        this.key = key;
        this.value = value;
        this.riskScore = 0;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    @Override
    public String toString() {
        return "Token{" +
                "key='" + key + '\'' +
                ", value='" + (riskScore > 2 ? "*****" : value) + '\'' +
                ", riskScore=" + riskScore +
                '}';
    }
}
