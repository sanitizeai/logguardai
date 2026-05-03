package com.logguardai.layout;

import com.logguardai.ai.AIConfig;
import com.logguardai.ai.AIService;
import com.logguardai.client.AIServiceFactory;
import com.logguardai.client.CachedAIService;
import com.logguardai.exception.ExceptionProcessor;
import com.logguardai.model.Token;
import com.logguardai.sanitizer.SanitizationEngine;
import com.logguardai.scoring.DecisionEngine;
import com.logguardai.scoring.RiskScoringEngine;
import com.logguardai.tokenizer.LogTokenizer;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.config.Node;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

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
 * samplingRate="0.1"/>
 */
@Plugin(name = "LogGuardLayout", category = Node.CATEGORY, elementType = "layout", printObject = true)
public class LogGuardLayout extends AbstractStringLayout {

    private final LogTokenizer tokenizer;
    private final RiskScoringEngine riskScoringEngine;
    private final DecisionEngine decisionEngine;
    private final SanitizationEngine sanitizationEngine;
    private final ExceptionProcessor exceptionProcessor;

    private final boolean aiEnabled;
    private final AIService aiService;
    private final int aiThreshold;
    private final long aiTimeoutMs;
    private final double samplingRate;
    private final List<String> safeKeyPatterns;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public LogGuardLayout(Charset charset, boolean aiEnabled, String aiProvider, String aiApiKey,
            String aiModel, int aiThreshold, long aiTimeoutMs, double samplingRate, List<String> safeKeyPatterns) {
        super(charset);
        this.aiEnabled = aiEnabled;
        this.aiThreshold = aiThreshold;
        this.aiTimeoutMs = aiTimeoutMs;
        this.samplingRate = samplingRate;
        this.safeKeyPatterns = safeKeyPatterns != null ? safeKeyPatterns : new ArrayList<>();

        // Initialize components
        this.tokenizer = new LogTokenizer();
        this.riskScoringEngine = new RiskScoringEngine(safeKeyPatterns);
        this.decisionEngine = new DecisionEngine();
        this.sanitizationEngine = new SanitizationEngine();
        this.exceptionProcessor = new ExceptionProcessor();

        // Initialize AI service if enabled
        if (aiEnabled && aiApiKey != null && !aiApiKey.isEmpty()) {
            AIConfig aiConfig = new AIConfig();
            aiConfig.setApiProvider(aiProvider != null ? aiProvider : "openai");
            aiConfig.setApiKey(aiApiKey);
            aiConfig.setModel(aiModel != null ? aiModel : "gpt-3.5-turbo");
            aiConfig.setTimeoutMs(aiTimeoutMs);
            this.aiService = AIServiceFactory.createService(aiConfig);
        } else {
            this.aiService = AIServiceFactory.createService(null); // No-op service
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
            for (Token token : tokens) {
                DecisionEngine.SanitizationAction action = decisionEngine.decideSanitization(token.getRiskScore());
                String originalPair = token.getKey() + "=" + token.getValue();

                if (action == DecisionEngine.SanitizationAction.RULE_BASED_MASK) {
                    // Apply rule-based masking
                    String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                    result = result.replace(originalPair, maskedPair);
                } else if (action == DecisionEngine.SanitizationAction.AI_SANITIZE && aiEnabled) {
                    // Use AI sanitization if enabled and sampled
                    if (shouldSampleForAI()) {
                        try {
                            String aiSanitized = aiService.sanitize(token.getValue(), token.getKey());
                            result = result.replace(token.getValue(), aiSanitized);
                        } catch (Exception e) {
                            // Fallback to rule-based masking on AI error
                            String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                            result = result.replace(originalPair, maskedPair);
                        }
                    } else {
                        // Not sampled, use rule-based masking
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
     * Supports both v0.1 (rule-based only) and v0.2 (with AI) configurations.
     * 
     * Attributes:
     * - safeKeyPatterns: CSV of regex patterns for keys that should NOT be sanitized
     *   Example: ".*correlation.*,x-.*-id,trace.*"
     */
    @PluginFactory
    public static LogGuardLayout createLayout(
            @PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset,
            @PluginAttribute(value = "aiEnabled", defaultString = "false") boolean aiEnabled,
            @PluginAttribute(value = "aiProvider", defaultString = "openai") String aiProvider,
            @PluginAttribute(value = "aiApiKey", defaultString = "") String aiApiKey,
            @PluginAttribute(value = "aiModel", defaultString = "gpt-3.5-turbo") String aiModel,
            @PluginAttribute(value = "aiThreshold", defaultString = "5") int aiThreshold,
            @PluginAttribute(value = "aiTimeoutMs", defaultString = "2000") long aiTimeoutMs,
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

        return new LogGuardLayout(charset, aiEnabled, aiProvider, aiApiKey, aiModel,
                aiThreshold, aiTimeoutMs, samplingRate, safeKeyPatterns);
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=" + this.getCharset();
    }
}
