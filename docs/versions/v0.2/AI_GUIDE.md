# v0.2 AI Integration Guide

A comprehensive guide for using LogGuardAI v0.2 with AI-powered sanitization, caching, and intelligent exception handling.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Configuration](#configuration)
3. [AI Features](#ai-features)
4. [Caching Strategy](#caching-strategy)
5. [Examples](#examples)
6. [Troubleshooting](#troubleshooting)
7. [Advanced Usage](#advanced-usage)

---

## Quick Start

### 1. Install v0.2

```xml
<dependency>
    <groupId>com.logguardai</groupId>
    <artifactId>logguardai</artifactId>
    <version>0.2.0</version>
</dependency>
```

### 2. Get OpenAI API Key

```bash
# Visit https://platform.openai.com/account/api-keys
# Create a new secret key
# Copy the key (you'll only see it once!)
```

### 3. Add Environment Variable

```bash
# Linux/Mac
export OPENAI_API_KEY="sk-..."

# Windows (PowerShell)
$env:OPENAI_API_KEY = "sk-..."

# In Docker/Kubernetes, use secrets management
```

### 4. Configure log4j2.xml

```xml
<Configuration>
    <Appenders>
        <Console name="Console">
            <LogGuardLayout
                aiEnabled="true"
                aiProvider="openai"
                aiApiKey="${OPENAI_API_KEY}"
                aiModel="gpt-3.5-turbo"
                aiThreshold="5"
                aiTimeoutMs="2000"
                samplingRate="0.1"/>
        </Console>
    </Appenders>
    <Root level="info">
        <AppenderRef ref="Console"/>
    </Root>
</Configuration>
```

### 5. Use It!

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyApp {
    private static final Logger logger = LogManager.getLogger(MyApp.class);

    public static void main(String[] args) {
        // High-risk data automatically gets AI sanitization!
        String apiKey = "sk-abcdef123456789xyz";
        logger.info("API authentication: key={}", apiKey);
        // 10% chance: AI generates smart masking
    }
}
```

---

## Configuration

### Full Configuration Options

```xml
<LogGuardLayout
    <!-- Core Settings -->
    charset="UTF-8"
    
    <!-- AI Enablement -->
    aiEnabled="true"                      <!-- Enable/disable AI -->
    aiProvider="openai"                   <!-- AI provider -->
    aiApiKey="${OPENAI_API_KEY}"         <!-- API credentials -->
    aiModel="gpt-3.5-turbo"              <!-- Model: gpt-3.5-turbo or gpt-4 -->
    
    <!-- Risk & Sampling -->
    aiThreshold="5"                       <!-- Score for AI activation -->
    samplingRate="0.1"                    <!-- 10% of high-risk get AI -->
    
    <!-- Performance -->
    aiTimeoutMs="2000"/>                  <!-- 2 second API timeout -->
```

### Configuration Profiles

**Development (Fast, Cheap):**
```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="0.05"          <!-- 5% sampling -->
    aiTimeoutMs="1000"/>         <!-- 1 second timeout -->
```

**Production (Balanced):**
```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="0.1"           <!-- 10% sampling -->
    aiTimeoutMs="2000"/>         <!-- 2 second timeout -->
```

**High Security (Expensive):**
```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="0.5"           <!-- 50% sampling -->
    aiModel="gpt-4"              <!-- Use GPT-4 for quality -->
    aiTimeoutMs="3000"/>         <!-- 3 second timeout -->
```

**Unsampled (Fast, Cheap):**
```xml
<LogGuardLayout
    aiEnabled="false"/>          <!-- Use v0.1 behavior -->
```

---

## AI Features

### 1. AI-Based Sanitization

**What it does:**
- Analyzes sensitive values contextually
- Generates appropriate masking based on type
- Maintains structure and readability

**Example:**

```java
// Input
logger.info("Payment: creditCard=4532123456789012 cvv=123 exp=12/25");

// Rule-based (v0.1):
// Output: creditCard=***** cvv=***** exp=*****

// AI-based (v0.2, sampled):
// Output: creditCard=[CC_HIDDEN_*9012] cvv=[CVV_MASKED] exp=[12/25_MASKED]
// (More intelligent, preserves partial info safely)
```

### 2. Exception Explanation

**What it does:**
- Analyzes exception type, message, and stack trace
- Generates developer-friendly explanation
- Suggests likely fixes
- Cached for repeated exception types

**Example:**

```java
try {
    String json = fetchUserData(userId);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    String name = obj.get("name").getAsString();  // NPE if "name" missing
} catch (NullPointerException e) {
    logger.error("Failed to get user name", e);
}

// Output with AI:
// Exception: NullPointerException - null
// Insight: "JSON field 'name' was missing or null. Attempted to call 
// getAsString() on null. Fix: Use getAsJsonObject().has() to check 
// field existence before accessing, or use Optional pattern."
```

### 3. Data Classification

**What it does:**
- Classifies values as PII, sensitive, public, or unknown
- Used internally for masking decisions
- Helps understand data flow

**Supported Classes:**
- `pii` - Personally Identifiable Information
- `sensitive` - Confidential but not strictly PII
- `public` - Safe to log unmasked
- `unknown` - Could't determine

---

## Caching Strategy

### How Caching Works

```
First Log:
logger.info("userId=12345 token=abc123xyz");
  → Tokenize: [userId, token]
  → Score: [2, 6] (high risk)
  → Sample: Yes (random)
  → API call to OpenAI → "ABC123 is a common pattern..."
  → Cache result: "token:abc123xyz:token" → "[TOKEN_MASKED]"
  → Return result

Second Log (same token):
logger.info("userId=67890 token=abc123xyz");
  → Tokenize: [userId, token]
  → Score: [2, 6] (high risk)
  → Sample: Maybe (random)
  → Cache hit! → Instant return "[TOKEN_MASKED]"
  → No API call needed!
```

### Cache Configuration

**Cache Parameters:**
- **Default size:** 1000 entries
- **Default TTL:** 1 hour (3,600,000 ms)
- **Thread-safe:** Yes
- **Eviction:** LRU (Least Recently Used)

**Cache Key Format:**
```
sanitize:VALUE:CONTEXT
exception:EXCEPTION_TYPE:MESSAGE
classify:VALUE:CONTEXT
```

### Cache Benefits

| Metric | Impact |
|--------|--------|
| **API Call Reduction** | 80-95% fewer calls for similar data |
| **Latency** | 1ms vs 1500ms (1500x faster) |
| **Cost** | Proportional to sampling rate (1-2% of typical API costs) |
| **Memory** | ~1KB per cached entry × 1000 = 1MB |

### Monitoring Cache Performance

```java
// During runtime, log cache stats
if (aiService instanceof CachedAIService) {
    CachedAIService cached = (CachedAIService) aiService;
    LRUCache.CacheStats stats = cached.getCacheStats();
    
    logger.info("Cache: {}", stats);
    // Output: CacheStats{size=456/1000, ttl=3600000ms}
    
    // Clear cache if needed
    cached.clearCache();
}
```

---

## Examples

### Example 1: E-Commerce Application

```xml
<!-- log4j2.xml -->
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    samplingRate="0.2"/>  <!-- 20% for high-risk transactions -->
```

```java
public class OrderService {
    private static final Logger logger = LogManager.getLogger();

    public void processPayment(PaymentInfo payment) {
        // These will be intelligently sanitized
        logger.info("Processing order: " +
            "customerId={} " +
            "creditCard={} " +
            "cvv={} " +
            "amount={} " +
            "currency={}",
            payment.customerId,
            payment.creditCard,
            payment.cvv,
            payment.amount,
            payment.currency);
    }
}

// Output (with AI, sampled):
// Processing order: customerId=CUST_###45 creditCard=[CARD_ENDING_9012] 
// cvv=[CVV_MASKED] amount=99.99 currency=USD
```

### Example 2: Authentication Service

```java
public class AuthService {
    private static final Logger logger = LogManager.getLogger();

    public Token generateToken(User user) {
        Token token = tokenGenerator.generate(user);
        
        // Log with sanitization
        logger.info("Token generated: userId={} token={} expiresIn={}",
            user.getId(),
            token.getValue(),
            token.getExpiresIn());
        
        // AI will detect 'token' key and apply smart masking
        return token;
    }

    public void handleAuthFailure(String username, String reason, Exception e) {
        // Exception insights + error reason
        logger.error("Authentication failed for user: {} reason: {}",
            username, reason, e);
        
        // If NullPointerException: AI explains null access point
        // If TimeoutException: AI suggests timeout handling
        // If InvalidTokenException: AI suggests token validation
    }
}
```

### Example 3: API Gateway

```java
public class APIGateway {
    private static final Logger logger = LogManager.getLogger();

    public Response handleRequest(Request request) {
        logger.info("Request: path={} method={} apiKey={} userId={}",
            request.path,
            request.method,
            request.apiKey,          // AI-sanitized (detected as token)
            request.userId);          // Might be sensitive

        try {
            return processRequest(request);
        } catch (TimeoutException e) {
            logger.error("Request timeout: path={} duration={}ms",
                request.path, request.duration, e);
        }
    }
}

// Output:
// Request: path=/api/users method=GET apiKey=[API_KEY_HIDDEN] userId=[USER_ID]
// OR after exception:
// Exception: TimeoutException - Operation timed out after 5000ms
// Insight: "API call exceeded 5s timeout. Likely slow backend or network issue.
//           Try increasing timeout, checking service health, or adding retries."
```

---

## Troubleshooting

### "AI not being invoked"

**Check:**
```xml
<!-- 1. AI is enabled -->
<LogGuardLayout aiEnabled="true"/>

<!-- 2. API key is set -->
<LogGuardLayout aiApiKey="${OPENAI_API_KEY}"/>

<!-- 3. Sampling rate allows it -->
<!-- Default: 5%, increase to test -->
<LogGuardLayout samplingRate="1.0"/>  <!-- 100% -->

<!-- 4. Risk score is high enough -->
<!-- Default threshold: 5 -->
<!-- Check if your data gets risk score > 5 -->
```

### "API 401 Error"

```
Error: "Incorrect API key provided"
```

**Fix:**
```bash
# 1. Verify key is correct
echo $OPENAI_API_KEY

# 2. Check key is not expired (regenerate if needed)
# Visit https://platform.openai.com/account/api-keys

# 3. Ensure key is passed correctly
<LogGuardLayout aiApiKey="${OPENAI_API_KEY}"/>  # Uses environment variable

# 4. Test directly
curl -H "Authorization: Bearer YOUR_KEY" \
  https://api.openai.com/v1/models
```

### "Timeout errors"

```
Error: "health check timed out"
```

**Fix:**
```xml
<!-- 1. Increase timeout -->
<LogGuardLayout aiTimeoutMs="5000"/>  <!-- 5 seconds -->

<!-- 2. Disable API key validation (health check) -->
<!-- Skip API key in config, it won't be used -->

<!-- 3. Use direct key (no env variable lookup) -->
<LogGuardLayout aiApiKey="sk-your-key-here"/>

<!-- 4. Disable AI if network is unreliable -->
<LogGuardLayout aiEnabled="false"/>
```

### "High API costs"

```
Issue: Unexpected OpenAI charges
```

**Fix:**
```xml
<!-- 1. Reduce sampling rate -->
<LogGuardLayout samplingRate="0.01"/>  <!-- 1% instead of 10% -->

<!-- 2. Use cheaper model -->
<LogGuardLayout aiModel="gpt-3.5-turbo"/>  <!-- Not gpt-4 -->

<!-- 3. Reduce max tokens -->
<!-- In code, adjust AIConfig.maxTokens (default: 150) -->

<!-- 4. Increase cache TTL -->
<!-- More cache hits = fewer API calls -->

<!-- 5. Monitor costs -->
<!-- Check OpenAI dashboard: https://platform.openai.com/account/billing/overview -->
```

---

## Advanced Usage

### Custom AI Service Implementation

Implement your own AI service:

```java
import com.logguardai.ai.AIService;
import com.logguardai.ai.AIServiceException;

public class CustomAIService implements AIService {
    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        // Your custom logic here
        return "[CUSTOM_MASKED]";
    }

    @Override
    public String explainException(String exceptionType, String message, String stackTraceSnippet) {
        // Custom exception explanation logic
        return "Your explanation here";
    }

    @Override
    public String classifyData(String value, String context) {
        // Custom classification logic
        return "pii";  // or "sensitive", "public", "unknown"
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getServiceName() {
        return "My Custom AI Service";
    }
}
```

Then use it:

```java
AIService customService = new CustomAIService();
AIService cachedService = AIServiceFactory.withCaching(customService, 1000, 3600000);
// Now use cachedService in LogGuardLayout
```

### Programmatic Configuration

```java
import com.logguardai.ai.AIConfig;
import com.logguardai.client.AIServiceFactory;
import com.logguardai.ai.AIService;

// Create config programmatically
AIConfig config = new AIConfig();
config.setApiProvider("openai");
config.setApiKey(System.getenv("OPENAI_API_KEY"));
config.setModel("gpt-3.5-turbo");
config.setMaxTokens(150);
config.setTemperature(0.3);
config.setTimeoutMs(2000);

// Create service
AIService service = AIServiceFactory.createService(config);

// Check if healthy
if (service.isHealthy()) {
    // Use it
    String sanitized = service.sanitize("secret_value", "password");
}
```

---

## Performance Tuning

### Low-Latency Scenario

```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="0.01"           <!-- 1% AI -->
    aiTimeoutMs="1000"            <!-- Fast fail -->
    aiThreshold="6"/>             <!-- Only highest risk -->
```

### High-Security Scenario

```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="1.0"            <!-- 100% AI -->
    aiModel="gpt-4"               <!-- Best quality -->
    aiTimeoutMs="5000"/>          <!-- More time allowed -->
```

### Cost-Optimized Scenario

```xml
<LogGuardLayout
    aiEnabled="true"
    samplingRate="0.02"           <!-- 2% AI -->
    aiModel="gpt-3.5-turbo"       <!-- Cheaper model -->
    aiThreshold="6"/>             <!-- Only highest risk -->
```

---

## Monitoring & Debugging

### Enable Debug Logging

```xml
<Loggers>
    <Logger name="com.logguardai" level="debug">
        <AppenderRef ref="Console"/>
    </Logger>
</Loggers>
```

### Monitor Cache Hit Rate

```java
// Periodic reporting
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    if (aiService instanceof CachedAIService) {
        LRUCache.CacheStats stats = ((CachedAIService) aiService).getCacheStats();
        logger.info("Cache stats: {}", stats);
    }
}, 0, 5, TimeUnit.MINUTES);
```

---

## Cost Estimation

### API Usage

```
Typical traffic: 10,000 logs/minute

With v0.2:
- Rule-based: ~9,500 logs (95%) - No cost
- Sampled AI: ~500 logs (5%) - Cost

Cost per 1M calls:
- GPT-3.5-turbo input: $0.0015
- GPT-3.5-turbo output: $0.002
- Estimated: ~$2 per million logs
```

### Monthly Estimate

```
Traffic: 10 million logs/month
Sampling: 5%
AI calls: 500k/month

GPT-3.5-turbo cost:
- Input tokens: ~50M tokens @ $0.0015 = $75
- Output tokens: ~10M tokens @ $0.002 = $20
- Total: ~$95/month

With caching (assume 80% cache hit):
- Actual API calls: 100k
- Cost: ~$19/month (80% savings!)
```

---

## Support & Community

- **GitHub:** https://github.com/sanitizeai/logguardai
- **Issues:** https://github.com/sanitizeai/logguardai/issues
- **Discussions:** https://github.com/sanitizeai/logguardai/discussions

---

**Ready to supercharge your logging? Get started with v0.2 today! 🚀**
