package com.logguardai.layout;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.client.AIServiceFactory;
import com.logguardai.exception.ExceptionProcessor;
import com.logguardai.metrics.MetricsConfig;
import com.logguardai.metrics.MetricsFileWriter;
import com.logguardai.metrics.MetricsFlushManager;
import com.logguardai.metrics.MetricsPattern;
import com.logguardai.metrics.MetricsRegistry;
import com.logguardai.model.Token;
import com.logguardai.sanitizer.SanitizationEngine;
import com.logguardai.scoring.DecisionEngine;
import com.logguardai.scoring.RiskScoringEngine;
import com.logguardai.tokenizer.LogTokenizer;

/**
 * LogGuardLayout - Main Log4j2 plugin for sanitizing logs.
 * 
 * This layout processes log events through a sanitization pipeline:
 * 1. Tokenize message
 * 2. Score tokens for risk
 * 3. Decide sanitization action
 * 4. Apply rule-based masking OR AI sanitization (v0.2+)
 * 5. Append exception insights (with optional AI enhancement)
 * 
 * Configuration example (v0.2):
 * <LogGuardLayout
 * aiEnabled="true"
 * aiProvider="openai"
 * aiApiKey="${OPENAI_API_KEY}"
 * aiThreshold="5"
 * aiTimeoutMs="2000"
 * aiAsyncWaitMs="100"
 * batchSize="5"
 * samplingRate="0.1"
 * extractMetrics="true"/>
 */
@Plugin(name = "LogGuardLayout", category = Node.CATEGORY, elementType = "layout", printObject = true)
public class LogGuardLayout extends AbstractStringLayout {

    private final LogTokenizer tokenizer;
    private final RiskScoringEngine riskScoringEngine;
    private final DecisionEngine decisionEngine;
    private final SanitizationEngine sanitizationEngine;
    private final ExceptionProcessor exceptionProcessor;

    private final boolean aiEnabled;
    private final boolean extractMetrics;
    private final AIService aiService;
    private final int aiThreshold;
    private final long aiTimeoutMs;
    private final long aiAsyncWaitMs;
    private final int batchSize;
    private final double samplingRate;
    private final List<String> safeKeyPatterns;
    
    private final MetricsRegistry metricsRegistry;
    private final MetricsFlushManager metricsFlushManager;
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public LogGuardLayout(Charset charset, boolean aiEnabled, boolean extractMetrics, AIConfig aiConfig,
            int aiThreshold, long aiAsyncWaitMs, int batchSize, double samplingRate, List<String> safeKeyPatterns,
            MetricsConfig metricsConfig) {
        super(charset);
        this.aiEnabled = aiEnabled;
        this.extractMetrics = extractMetrics;
        this.aiThreshold = aiThreshold;
        this.aiTimeoutMs = aiConfig != null ? aiConfig.getTimeoutMs() : 2000;
        this.aiAsyncWaitMs = aiAsyncWaitMs;
        this.batchSize = batchSize;
        this.samplingRate = samplingRate;
        this.safeKeyPatterns = safeKeyPatterns != null ? safeKeyPatterns : new ArrayList<>();

        // Initialize components
        this.tokenizer = new LogTokenizer();
        this.riskScoringEngine = new RiskScoringEngine(safeKeyPatterns);
        this.decisionEngine = new DecisionEngine();
        this.sanitizationEngine = new SanitizationEngine();
        this.exceptionProcessor = new ExceptionProcessor();

        // Initialize AI service if enabled
        if (aiEnabled && aiConfig != null && aiConfig.isConfigured()) {
            this.aiService = AIServiceFactory.createService(aiConfig);
        } else {
            this.aiService = AIServiceFactory.createService(null); // No-op service
        }
        
        // Initialize metrics if enabled
        if (extractMetrics && metricsConfig != null && metricsConfig.isConfigured()) {
            this.metricsRegistry = new MetricsRegistry(metricsConfig.getMaxCardinalityPerPattern());
            this.metricsRegistry.addPatterns(metricsConfig.getPatterns());
            this.metricsFlushManager = new MetricsFlushManager(
                    metricsRegistry,
                    new MetricsFileWriter(metricsConfig.getFilePath(), metricsRegistry),
                    metricsConfig.getFlushIntervalMs());
            this.metricsFlushManager.start();
        } else {
            this.metricsRegistry = null;
            this.metricsFlushManager = null;
        }
    }

    /**
     * Main layout method called by Log4j2 for each log event.
     */
    @Override
    public String toSerializable(LogEvent event) {
        StringBuilder output = new StringBuilder();

        // Format: timestamp [level] logger - message [exception]
        output.append(formatTimestamp(event.getTimeMillis()));
        output.append(" [").append(event.getLevel()).append("] ");
        output.append(event.getLoggerName()).append(" - ");

        // Sanitize message
        String sanitizedMessage = sanitizeMessage(event.getMessage().getFormattedMessage());
        output.append(sanitizedMessage);

        // Add exception insights if present
        if (event.getThrown() != null) {
            output.append("\n");
            output.append(processException(event.getThrown()));
        }

        output.append("\n");
        
        // Record metrics if enabled
        if (extractMetrics && metricsRegistry != null) {
            try {
                // Record the original unmodified message for pattern matching
                String originalMsg = event.getMessage().getFormattedMessage();
                metricsRegistry.recordLogLine(originalMsg);
            } catch (Exception e) {
                // Silently ignore metric recording failures
            }
        }
        
        return output.toString();
    }

    /**
     * Main sanitization pipeline with AI integration.
     */
    private String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        try {
            // Step 1: Tokenize
            List<Token> tokens = tokenizer.tokenize(message);

            if (tokens.isEmpty()) {
                // No tokens extracted, return as-is
                return message;
            }

            // Step 2: Score tokens
            riskScoringEngine.scoreTokens(tokens);

            // Step 3-4: Decide and sanitize
            String result = message;
            
            // Separate tokens by sanitization method
            List<Token> ruleBasedTokens = new ArrayList<>();
            List<Token> aiTokens = new ArrayList<>();
            
            for (Token token : tokens) {
                DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(token.getRiskScore());
                
                if (action == DecisionEngine.SanitizationAction.RULE_BASED_MASK) {
                    ruleBasedTokens.add(token);
                } else if (action == DecisionEngine.SanitizationAction.AI_SANITIZE && aiEnabled && shouldSampleForAI()) {
                    aiTokens.add(token);
                } else {
                    // Default to rule-based masking for any other case
                    ruleBasedTokens.add(token);
                }
            }
            
            // Apply rule-based masking
            for (Token token : ruleBasedTokens) {
                String originalPair = token.getKey() + "=" + token.getValue();
                String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                result = result.replace(originalPair, maskedPair);
            }
            
            // Apply AI sanitization in batches
            if (!aiTokens.isEmpty()) {
                try {
                    Map<String, String> aiResults = processTokensWithAI(aiTokens);
                    for (Map.Entry<String, String> entry : aiResults.entrySet()) {
                        result = result.replace(entry.getKey(), entry.getValue());
                    }
                } catch (Exception e) {
                    // Fallback: apply rule-based masking to AI tokens on error
                    for (Token token : aiTokens) {
                        String originalPair = token.getKey() + "=" + token.getValue();
                        String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                        result = result.replace(originalPair, maskedPair);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            // Fail-safe: always return original message rather than breaking logging
            System.err.println("LogGuardAI: Error during sanitization, returning original message: " + e.getMessage());
            return message;
        }
    }

    /**
     * Process exception and generate insights (with optional AI enhancement).
     */
    private String processException(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringBuilder exceptionOutput = new StringBuilder();
        exceptionOutput.append("Exception: ").append(exceptionProcessor.getExceptionSummary(throwable)).append("\n");

        // Use AI for explanation if enabled and sampled
        String insight;
        if (aiEnabled && shouldSampleForAI() && aiService.isHealthy()) {
            try {
                String stackSnippet = exceptionProcessor.getStackTraceSummary(throwable, 2);
                insight = aiService.explainException(
                        throwable.getClass().getName(),
                        throwable.getMessage() != null ? throwable.getMessage() : "",
                        stackSnippet);
            } catch (Exception e) {
                // Fallback to rule-based explanation
                insight = exceptionProcessor.generateInsight(throwable);
            }
        } else {
            // Use rule-based explanation
            insight = exceptionProcessor.generateInsight(throwable);
        }

        exceptionOutput.append("Insight: ").append(insight).append("\n");
        exceptionOutput.append("Stack Trace:\n");
        exceptionOutput.append(exceptionProcessor.getStackTraceSummary(throwable, 5));

        return exceptionOutput.toString();
    }

    /**
     * Process tokens requiring AI sanitization in batches.
     */
    private Map<String, String> processTokensWithAI(List<Token> aiTokens) throws Exception {
        Map<String, String> results = new HashMap<>();
        
        // Process tokens in batches
        for (int i = 0; i < aiTokens.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, aiTokens.size());
            List<Token> batch = aiTokens.subList(i, endIndex);
            
            // Extract values and contexts for this batch
            List<String> values = new ArrayList<>();
            List<String> contexts = new ArrayList<>();
            for (Token token : batch) {
                values.add(token.getValue());
                contexts.add(token.getKey());
            }
            
            try {
                // Call batch AI sanitization
                java.util.concurrent.CompletableFuture<Map<String, String>> future = aiService.sanitizeBatchAsync(values, contexts);
                Map<String, String> batchResults = future.get(Math.max(0, Math.min(aiAsyncWaitMs, aiTimeoutMs)), TimeUnit.MILLISECONDS);
                
                // Map results back to original values
                for (Token token : batch) {
                    String sanitized = batchResults.get(token.getValue());
                    if (sanitized != null) {
                        results.put(token.getValue(), sanitized);
                    } else {
                        // Fallback to rule-based masking if AI didn't return this value
                        results.put(token.getValue(), sanitizationEngine.maskValue(token.getValue()));
                    }
                }
                
            } catch (TimeoutException e) {
                // Batch timeout - fallback to rule-based masking for this batch
                for (Token token : batch) {
                    results.put(token.getValue(), sanitizationEngine.maskValue(token.getValue()));
                }
            } catch (InterruptedException | ExecutionException e) {
                // Batch error - fallback to rule-based masking for this batch
                for (Token token : batch) {
                    results.put(token.getValue(), sanitizationEngine.maskValue(token.getValue()));
                }
            }
        }
        
        return results;
    }

    /**
     * Determine if this call should use AI (based on sampling rate).
     */
    private boolean shouldSampleForAI() {
        if (samplingRate >= 1.0) {
            return true;
        }
        if (samplingRate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < samplingRate;
    }

    /**
     * Format timestamp for output.
     */
    private String formatTimestamp(long timeMillis) {
        return DATE_FORMAT.format(new Date(timeMillis));
    }

    /**
     * Factory method for Log4j2 plugin creation.
     * Supports v0.1 (rule-based), v0.2 (with AI), and v0.5+ (pattern-based metrics).
     * 
     * Attributes:
     * - safeKeyPatterns: CSV of regex patterns for keys that should NOT be sanitized
     *   Example: ".*correlation.*,x-.*-id,trace.*"
     * - metricsFilePath: Path to metrics output file (default: logs/metrics.txt)
     * - metricsFlushIntervalMs: Interval for periodic flush (default: 60000)
     * - metricsMaxCardinality: Max unique label combos per pattern (default: 10000)
     * - metricsPatterns: Semicolon-separated pattern definitions
     *   Format: "name|regex|metricName|field1,field2;name2|regex2|metric2|field1"
     *   Example: "http_get|GET /([\\w/]+)|http_requests_total|endpoint"
     */
    @PluginFactory
    public static LogGuardLayout createLayout(
            @PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset,
            @PluginAttribute(value = "aiEnabled", defaultString = "false") boolean aiEnabled,
            @PluginAttribute(value = "aiProvider", defaultString = "openai") String aiProvider,
            @PluginAttribute(value = "aiApiKey", defaultString = "") String aiApiKey,
            @PluginAttribute(value = "aiModel", defaultString = "gpt-3.5-turbo") String aiModel,
            @PluginAttribute(value = "azureEndpoint", defaultString = "") String azureEndpoint,
            @PluginAttribute(value = "azureDeployment", defaultString = "") String azureDeployment,
            @PluginAttribute(value = "azureApiVersion", defaultString = "2023-12-01") String azureApiVersion,
            @PluginAttribute(value = "extractMetrics", defaultString = "false") boolean extractMetrics,
            @PluginAttribute(value = "metricsFilePath", defaultString = "logs/metrics.txt") String metricsFilePath,
            @PluginAttribute(value = "metricsFlushIntervalMs", defaultString = "60000") long metricsFlushIntervalMs,
            @PluginAttribute(value = "metricsMaxCardinality", defaultString = "10000") int metricsMaxCardinality,
            @PluginAttribute(value = "metricsPatterns", defaultString = "") String metricsPatterns,
            @PluginAttribute(value = "aiThreshold", defaultString = "5") int aiThreshold,
            @PluginAttribute(value = "aiTimeoutMs", defaultString = "2000") long aiTimeoutMs,
            @PluginAttribute(value = "aiAsyncWaitMs", defaultString = "100") long aiAsyncWaitMs,
            @PluginAttribute(value = "batchSize", defaultString = "5") int batchSize,
            @PluginAttribute(value = "samplingRate", defaultString = "0.05") double samplingRate,
            @PluginAttribute(value = "safeKeyPatterns", defaultString = "") String safeKeyPatternsStr) {

        // Parse safe key patterns from CSV string
        List<String> safeKeyPatterns = new ArrayList<>();
        if (safeKeyPatternsStr != null && !safeKeyPatternsStr.isEmpty()) {
            String[] patterns = safeKeyPatternsStr.split(",");
            for (String pattern : patterns) {
                safeKeyPatterns.add(pattern.trim());
            }
        }

        // Create AI config with provider-specific settings
        AIConfig aiConfig = null;
        if (aiEnabled && aiApiKey != null && !aiApiKey.isEmpty()) {
            aiConfig = new AIConfig();
            aiConfig.setApiProvider(aiProvider);
            aiConfig.setApiKey(aiApiKey);
            aiConfig.setModel(aiModel);
            aiConfig.setTimeoutMs(aiTimeoutMs);

            // Set Azure-specific config if using Azure provider
            if ("azure-openai".equalsIgnoreCase(aiProvider)) {
                if (azureEndpoint != null && !azureEndpoint.isEmpty()) {
                    aiConfig.setAzureEndpoint(azureEndpoint);
                }
                if (azureDeployment != null && !azureDeployment.isEmpty()) {
                    aiConfig.setAzureDeployment(azureDeployment);
                } else {
                    // Default to model name if not specified
                    aiConfig.setAzureDeployment(aiModel);
                }
                aiConfig.setAzureApiVersion(azureApiVersion);
            }
        }

        // Create metrics config if enabled
        MetricsConfig metricsConfig = null;
        if (extractMetrics && metricsPatterns != null && !metricsPatterns.isEmpty()) {
            metricsConfig = new MetricsConfig();
            metricsConfig.setEnabled(true);
            metricsConfig.setFilePath(metricsFilePath);
            metricsConfig.setFlushIntervalMs(metricsFlushIntervalMs);
            metricsConfig.setMaxCardinalityPerPattern(metricsMaxCardinality);
            
            // Parse metrics patterns: "name|regex|metricName|field1,field2;name2|..."
            // Regex itself may contain alternation pipes (|), so parse by field positions.
            String[] patternDefinitions = metricsPatterns.split(";");
            for (String def : patternDefinitions) {
                def = def.trim();
                if (def.isEmpty()) {
                    continue;
                }
                
                int firstPipe = def.indexOf('|');
                int lastPipe = def.lastIndexOf('|');
                if (firstPipe < 0 || lastPipe < 0 || firstPipe == lastPipe) {
                    continue;
                }

                int secondLastPipe = def.lastIndexOf('|', lastPipe - 1);
                if (secondLastPipe <= firstPipe) {
                    continue;
                }

                String patternName = def.substring(0, firstPipe).trim();
                String regex = def.substring(firstPipe + 1, secondLastPipe).trim();
                String metricName = def.substring(secondLastPipe + 1, lastPipe).trim();
                String[] fieldNames = def.substring(lastPipe + 1).split(",");

                List<String> fields = new ArrayList<>();
                for (String field : fieldNames) {
                    fields.add(field.trim());
                }

                MetricsPattern pattern = new MetricsPattern(patternName, regex, metricName, fields);
                metricsConfig.addPattern(pattern);
            }
        }

        return new LogGuardLayout(charset, aiEnabled, extractMetrics, aiConfig,
                aiThreshold, aiAsyncWaitMs, batchSize, samplingRate, safeKeyPatterns, metricsConfig);
    }

    public MetricsRegistry getMetricsRegistry() {
        return metricsRegistry;
    }

    /**
     * Get the metrics flush manager for testing/monitoring.
     */
    public MetricsFlushManager getMetricsFlushManager() {
        return metricsFlushManager;
    }

    /**
     * Manually flush metrics to file.
     */
    public void flushMetrics() {
        if (metricsFlushManager != null) {
            metricsFlushManager.flush();
        }
    }

    /**
     * Shutdown metrics system (called on application shutdown).
     */
    public void shutdown() {
        if (metricsFlushManager != null && metricsFlushManager.isRunning()) {
            metricsFlushManager.stop();
        }
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=" + this.getCharset();
    }
}
