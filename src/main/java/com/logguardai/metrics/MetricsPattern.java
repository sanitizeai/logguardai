package com.logguardai.metrics;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Defines a pattern for extracting metrics from log entries.
 * 
 * Example: Parse HTTP request logs
 *   Pattern: "GET /api/users - 200"
 *   Regex: "([A-Z]+) ([/\\w]+) - (\\d+)"
 *   MetricName: "http_requests_total"
 *   AggregationFields: ["method", "endpoint", "status"]
 */
public class MetricsPattern {
    
    private String patternName;
    private String regex;
    private Pattern compiledPattern;
    private String metricName;
    private List<String> aggregationFields;
    private boolean enabled;

    public MetricsPattern() {
        this.enabled = true;
        this.aggregationFields = new ArrayList<>();
    }

    public MetricsPattern(String patternName, String regex, String metricName, List<String> aggregationFields) {
        this.patternName = patternName;
        this.regex = regex;
        this.compiledPattern = Pattern.compile(regex);
        this.metricName = metricName;
        this.aggregationFields = aggregationFields != null ? aggregationFields : new ArrayList<>();
        this.enabled = true;
    }

    public String getPatternName() {
        return patternName;
    }

    public void setPatternName(String patternName) {
        this.patternName = patternName;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
        this.compiledPattern = Pattern.compile(regex);
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public List<String> getAggregationFields() {
        return aggregationFields;
    }

    public void setAggregationFields(List<String> aggregationFields) {
        this.aggregationFields = aggregationFields != null ? aggregationFields : new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if this pattern matches the log line.
     * Returns map of aggregation field names to values if match, null otherwise.
     */
    public Map<String, String> match(String logLine) {
        if (!enabled || logLine == null || compiledPattern == null) {
            return null;
        }

        java.util.regex.Matcher matcher = compiledPattern.matcher(logLine);
        if (!matcher.find()) {
            return null;
        }

        // Extract capture groups and map to aggregation fields
        Map<String, String> labels = new LinkedHashMap<>();
        for (int i = 0; i < aggregationFields.size(); i++) {
            int groupIndex = i + 1;  // Group 0 is full match
            if (groupIndex <= matcher.groupCount()) {
                String fieldName = aggregationFields.get(i);
                String fieldValue = matcher.group(groupIndex);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    labels.put(fieldName, fieldValue);
                }
            }
        }

        return labels;
    }

    /**
     * Build Prometheus-style metric key from metric name and labels.
     * Example: "http_requests_total{method=\"GET\",status=\"200\"}"
     */
    public String buildMetricKey(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return metricName;
        }

        StringBuilder sb = new StringBuilder(metricName).append("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "MetricsPattern{" +
                "name='" + patternName + '\'' +
                ", metric='" + metricName + '\'' +
                ", fields=" + aggregationFields +
                ", enabled=" + enabled +
                '}';
    }
}
