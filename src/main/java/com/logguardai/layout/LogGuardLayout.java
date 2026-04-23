package com.logguardai.layout;

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

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * LogGuardLayout - Main Log4j2 plugin for sanitizing logs.
 * 
 * This layout processes log events through a sanitization pipeline:
 * 1. Tokenize message
 * 2. Score tokens for risk
 * 3. Decide sanitization action
 * 4. Apply rule-based masking
 * 5. Append exception insights
 * 
 * Configuration example:
 * <LogGuardLayout aiEnabled="false" riskThreshold="5" timeoutMs="150"/>
 */
@Plugin(name = "LogGuardLayout", category = "Layout", elementType = "Layout", printObject = true)
public class LogGuardLayout extends AbstractStringLayout {
    
    private final LogTokenizer tokenizer;
    private final RiskScoringEngine riskScoringEngine;
    private final DecisionEngine decisionEngine;
    private final SanitizationEngine sanitizationEngine;
    private final ExceptionProcessor exceptionProcessor;
    
    private final boolean aiEnabled;
    private final int aiThreshold;
    private final long timeoutMs;
    private final double samplingRate;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public LogGuardLayout(Charset charset, boolean aiEnabled, int aiThreshold, 
                        long timeoutMs, double samplingRate) {
        super(charset);
        this.aiEnabled = aiEnabled;
        this.aiThreshold = aiThreshold;
        this.timeoutMs = timeoutMs;
        this.samplingRate = samplingRate;
        
        // Initialize components
        this.tokenizer = new LogTokenizer();
        this.riskScoringEngine = new RiskScoringEngine();
        this.decisionEngine = new DecisionEngine();
        this.sanitizationEngine = new SanitizationEngine();
        this.exceptionProcessor = new ExceptionProcessor();
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
     * Main sanitization pipeline.
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
                
                if (action == DecisionEngine.SanitizationAction.RULE_BASED_MASK) {
                    // Apply rule-based masking
                    String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                    String originalPair = token.getKey() + "=" + token.getValue();
                    result = result.replace(originalPair, maskedPair);
                } 
                else if (action == DecisionEngine.SanitizationAction.AI_SANITIZE && aiEnabled) {
                    // For v0.1, we use rule-based masking for high-risk too
                    // v0.2 will integrate AI service
                    String maskedPair = sanitizationEngine.buildMaskedPair(token.getKey(), token.getValue());
                    String originalPair = token.getKey() + "=" + token.getValue();
                    result = result.replace(originalPair, maskedPair);
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
     * Process exception and generate insights.
     */
    private String processException(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringBuilder exceptionOutput = new StringBuilder();
        exceptionOutput.append("Exception: ").append(exceptionProcessor.getExceptionSummary(throwable)).append("\n");
        exceptionOutput.append("Insight: ").append(exceptionProcessor.generateInsight(throwable)).append("\n");
        exceptionOutput.append("Stack Trace:\n");
        exceptionOutput.append(exceptionProcessor.getStackTraceSummary(throwable, 5));
        
        return exceptionOutput.toString();
    }

    /**
     * Format timestamp for output.
     */
    private String formatTimestamp(long timeMillis) {
        return DATE_FORMAT.format(new Date(timeMillis));
    }

    /**
     * Factory method for Log4j2 plugin creation.
     */
    @PluginFactory
    public static LogGuardLayout createLayout(
            @PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset,
            @PluginAttribute(value = "aiEnabled", defaultString = "false") boolean aiEnabled,
            @PluginAttribute(value = "aiThreshold", defaultString = "5") int aiThreshold,
            @PluginAttribute(value = "timeoutMs", defaultString = "150") long timeoutMs,
            @PluginAttribute(value = "samplingRate", defaultString = "0.05") double samplingRate) {
        
        return new LogGuardLayout(charset, aiEnabled, aiThreshold, timeoutMs, samplingRate);
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=" + this.getCharset();
    }
}
