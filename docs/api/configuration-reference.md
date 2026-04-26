# Configuration Model Reference

Reference for all configuration properties and classes.

---

## 📦 Configuration Classes

### AIConfig

**Package:** `com.logguardai.ai`

Container for AI service configuration.

#### Properties

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `apiKey` | String | (required) | API key for AI provider |
| `apiProvider` | String | "openai" | Which provider to use |
| `model` | String | "gpt-3.5-turbo" | AI model to use |
| `maxTokens` | int | 150 | Max response tokens |
| `temperature` | double | 0.3 | Sampling temperature (0-2) |
| `timeoutMs` | long | 2000 | API timeout in milliseconds |
| `retryOnTimeout` | boolean | true | Retry if timeout occurs |
| `maxRetries` | int | 3 | Max retry attempts |

#### Methods

```java
// Getters/Setters
String getApiKey();
void setApiKey(String apiKey);

String getApiProvider();
void setApiProvider(String provider);

String getModel();
void setModel(String model);

int getMaxTokens();
void setMaxTokens(int tokens);

double getTemperature();
void setTemperature(double temp);

long getTimeoutMs();
void setTimeoutMs(long timeout);

boolean isRetryOnTimeout();
void setRetryOnTimeout(boolean retry);

int getMaxRetries();
void setMaxRetries(int retries);

// Validation
boolean isConfigured();  // true if ready to use
```

#### Example

```java
AIConfig config = new AIConfig();
config.setApiProvider("openai");
config.setApiKey(System.getenv("OPENAI_API_KEY"));
config.setModel("gpt-3.5-turbo");
config.setMaxTokens(150);
config.setTemperature(0.3);
config.setTimeoutMs(2000);
config.setMaxRetries(3);

if (config.isConfigured()) {
    AIService service = AIServiceFactory.createService(config);
}
```

---

## 🏗️ LogGuard Configuration

### LogGuardLayout

**Package:** `com.logguardai.layout`

Main Log4j2 plugin configuration.

#### Factory Method

```java
public static Layout createLayout(
    Charset charset,           // Character encoding
    boolean aiEnabled,         // Enable AI features
    String aiProvider,         // AI provider ("openai")
    String aiApiKey,           // API key
    String aiModel,            // Model to use
    int aiThreshold,           // Risk threshold for AI
    long aiTimeoutMs,          // API timeout
    double samplingRate        // Sampling rate (0.0-1.0)
)
```

#### XML Configuration

```xml
<LogGuardLayout
    charset="UTF-8"
    aiEnabled="false"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-3.5-turbo"
    aiThreshold="5"
    aiTimeoutMs="2000"
    samplingRate="0.1"/>
```

---

## 📋 Log4j2.xml Configuration Structure

### Minimal Setup

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout/>
    </Console>
  </Appenders>

  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

### Full Setup (v0.2 with AI)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="com.logguardai">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout
          charset="UTF-8"
          aiEnabled="true"
          aiProvider="openai"
          aiApiKey="${OPENAI_API_KEY}"
          aiModel="gpt-3.5-turbo"
          aiThreshold="5"
          aiTimeoutMs="2000"
          samplingRate="0.1"/>
    </Console>
  </Appenders>

  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

---

## 🎨 Environment-Specific Configurations

### Development Configuration

```xml
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout
          aiEnabled="true"
          aiProvider="openai"
          aiApiKey="${OPENAI_API_KEY}"
          aiModel="gpt-3.5-turbo"
          aiThreshold="5"
          aiTimeoutMs="1000"
          samplingRate="0.05"/>
    </Console>
  </Appenders>
  
  <Loggers>
    <Root level="debug">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

**Characteristics:**
- AI enabled for testing
- Low sampling (5%) to save costs
- Fast timeout (1 sec)
- Debug logging level

---

### Production Configuration

```xml
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
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
  
  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

**Characteristics:**
- AI enabled by default
- Balanced sampling (10%)
- Reasonable timeout (2 sec)
- Info logging level

---

### High-Security Configuration

```xml
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout
          aiEnabled="true"
          aiProvider="openai"
          aiApiKey="${OPENAI_API_KEY}"
          aiModel="gpt-4"
          aiThreshold="3"
          aiTimeoutMs="3000"
          samplingRate="1.0"/>
    </Console>
  </Appenders>
  
  <Loggers>
    <Root level="trace">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

**Characteristics:**
- GPT-4 (most capable)
- Higher threshold (3 - more aggressive)
- Longer timeout (3 sec)
- 100% AI processing
- Trace logging level

---

### Fast (Rule-Based) Configuration

```xml
<Configuration>
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout aiEnabled="false"/>
    </Console>
  </Appenders>
  
  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

**Characteristics:**
- Pure rule-based masking
- No AI (v0.1 behavior)
- Fastest (<5ms)
- No API calls

---

## 🔐 Security Properties

### API Key Handling

❌ **Hardcoded (NEVER do this):**
```xml
<LogGuardLayout aiApiKey="sk-abcdef123456789"/>
```

✅ **Environment variable (correct):**
```xml
<LogGuardLayout aiApiKey="${OPENAI_API_KEY}"/>
```

**Set environment variable:**
```bash
export OPENAI_API_KEY="sk-..."
java -jar myapp.jar
```

---

## ⚙️ Advanced Properties

### Sampling Rate Calculations

```
Sampling Rate → API Calls → Cost

5% (0.05)   → 500K/10M logs   → ~$19/month
10% (0.1)   → 1M/10M logs     → $38/month
25% (0.25)  → 2.5M/10M logs   → $95/month
50% (0.5)   → 5M/10M logs     → $190/month
100% (1.0)  → 10M logs        → $380/month
```

*Estimates for 10 million logs at GPT-3.5 pricing*

---

### Timeout Impact

| Timeout (ms) | Behavior | Use Case |
|--------------|----------|----------|
| 500 | Fast fail | Testing, dev |
| 1000 | Quick timeout | Monitoring |
| 2000 | Balanced | Production (default) |
| 3000 | Wait for GPT-4 | High-accuracy |
| 5000 | Very patient | Legacy systems |

---

### Temperature (Model Creativity)

| Value | Behavior | Use |
|-------|----------|-----|
| 0.0 | Deterministic | Consistent masking |
| 0.3 | Conservative | Default (recommended) |
| 0.7 | Balanced | More variety |
| 1.0 | Creative | Most variation |
| 2.0 | Maximum | Wildly different |

**Recommendation:** Keep at 0.3 for masking

---

## 🧪 Configuration Validation

### Check Configuration

```bash
# Verify environment variable set
echo $OPENAI_API_KEY

# Should output: sk-xxxxx

# Check XML syntax
xmllint --noout log4j2.xml

# Should output: nothing (valid) or errors
```

### Test Configuration

```java
AIConfig config = new AIConfig();
config.setApiKey(System.getenv("OPENAI_API_KEY"));

if (config.isConfigured()) {
    System.out.println("✅ Configuration ready");
} else {
    System.out.println("❌ Configuration missing fields");
}
```

---

## 📝 Configuration Checklist

- [ ] API key set in environment (not hardcoded)
- [ ] log4j2.xml has correct package declaration
- [ ] Charset specified (UTF-8 recommended)
- [ ] AI enabled/disabled as intended
- [ ] Provider set to "openai"
- [ ] Model chosen (gpt-3.5-turbo or gpt-4)
- [ ] Timeout reasonable (1-3 seconds typical)
- [ ] Sampling rate appropriate for volume
- [ ] Threshold matches risk tolerance (3-7 typical)
- [ ] Configuration file is valid XML

---

## 🔗 Related Documentation

- **Configuration Guide:** [Full guide](../guides/configuration.md)
- **Troubleshooting:** [Common issues](../guides/troubleshooting.md)
- **Quick Start:** [Get started](../versions/v0.2/QUICK_START.md)

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)

---

*Last Updated: 2026-04-23*

[← Back to API Reference](README.md)
