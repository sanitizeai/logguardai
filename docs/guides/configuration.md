# Configuration Reference

Complete guide to all LogGuardAI configuration options.

**Applies To:** All versions (v0.1+)

---

## 📋 Overview

LogGuardAI can be configured via:
1. **log4j2.xml** (most common)
2. **Programmatic API** (Java code)
3. **Environment variables** (for sensitive data)

---

## 🔧 log4j2.xml Configuration

### Minimal Setup

```xml
<LogGuardLayout/>
```

All defaults applied.

---

### Full Configuration (v0.2+)

```xml
<LogGuardLayout
    charset="UTF-8"
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-3.5-turbo"
    aiThreshold="5"
    aiTimeoutMs="2000"
    samplingRate="0.1"/>
```

---

## 📝 Configuration Parameters

### General Parameters

#### `charset`
- **Type:** String
- **Default:** `UTF-8`
- **Purpose:** Character encoding
- **Example:** `charset="UTF-8"`

### AI Parameters (v0.2+)

#### `aiEnabled`
- **Type:** Boolean
- **Default:** `false`
- **Purpose:** Enable/disable AI features
- **Example:** `aiEnabled="true"`
- **Note:** Set to `false` for v0.1 behavior

#### `aiProvider`
- **Type:** String
- **Default:** `openai`
- **Options:** `openai` (more providers coming)
- **Purpose:** Which AI provider to use
- **Example:** `aiProvider="openai"`

#### `aiApiKey`
- **Type:** String
- **Default:** (none)
- **Purpose:** API key for AI service
- **Example:** `aiApiKey="${OPENAI_API_KEY}"`
- **Security:** Use environment variable!

#### `aiModel`
- **Type:** String
- **Default:** `gpt-3.5-turbo`
- **Options:** `gpt-3.5-turbo`, `gpt-4`
- **Purpose:** Which AI model to use
- **Example:** `aiModel="gpt-3.5-turbo"`
- **Cost Note:** GPT-4 is more expensive

#### `aiThreshold`
- **Type:** Integer
- **Default:** `5`
- **Range:** 1-10
- **Purpose:** Risk score threshold for AI intervention
- **Logic:** Scores > threshold get AI sanitization
- **Example:** `aiThreshold="5"` (use AI for scores 6+)

#### `aiTimeoutMs`
- **Type:** Long
- **Default:** `2000` (2 seconds)
- **Unit:** Milliseconds
- **Purpose:** Timeout for AI API calls
- **Example:** `aiTimeoutMs="2000"`
- **Note:** Prevents hanging on slow APIs

#### `samplingRate`
- **Type:** Double
- **Default:** `1.0` (100%)
- **Range:** 0.0 to 1.0
- **Purpose:** Fraction of high-risk logs sent to AI
- **Example:** `samplingRate="0.1"` (10%)
- **Cost Optimization:** Lower = cheaper
- **Formula:** `cost = 95 * samplingRate` (approx, per 10M logs)

---

## 🎯 Configuration Profiles

### Development (AI Testing)

```xml
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-3.5-turbo"
    aiThreshold="5"
    aiTimeoutMs="1000"
    samplingRate="0.05"/>
```

**Characteristics:**
- AI enabled for experimentation
- 5% sampling (low cost)
- 1-second timeout (fast feedback)
- GPT-3.5 (cheaper model)

---

### Production (Balanced)

```xml
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-3.5-turbo"
    aiThreshold="5"
    aiTimeoutMs="2000"
    samplingRate="0.1"/>
```

**Characteristics:**
- AI enabled for high-risk logs
- 10% sampling (reasonable cost)
- 2-second timeout (reliability)
- GPT-3.5 (good balance)

---

### High-Security (Premium)

```xml
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-4"
    aiThreshold="3"
    aiTimeoutMs="3000"
    samplingRate="1.0"/>
```

**Characteristics:**
- All logs get AI analysis
- No sampling (highest security)
- 3-second timeout (wait for GPT-4)
- GPT-4 (most capable model)
- **Cost:** ~$95/month for 10M logs

---

### Fast (No AI)

```xml
<LogGuardLayout
    aiEnabled="false"/>
```

**Characteristics:**
- Pure rule-based masking
- Fast (<5ms)
- No API calls
- No cost
- **Equivalent to v0.1 behavior**

---

## 💰 Cost Examples

Based on 10 million logs/month at $0.005 per 1000 tokens:

### Scenario 1: Development (5% sampling)
```
Logs analyzed by AI: 500,000 (5%)
Avg tokens per call: 150
Total tokens: 75,000,000
Cost: $375 ÷ 5 = $75/month (with caching)
```

### Scenario 2: Production (10% sampling)
```
Logs analyzed by AI: 1,000,000 (10%)
Avg tokens per call: 150
Total tokens: 150,000,000
Cost: $750 ÷ 5 = $150/month (with caching)  [Note: Estimate only]
```

### Scenario 3: Caching Impact
```
Without caching:  $95/month (10% sampling)
Cache hit rate:   80%
Effective cost:   $19/month
Savings:          80%
```

---

## 🔐 Security Best Practices

### 1. Use Environment Variables for API Keys

❌ **Don't do this:**
```xml
<LogGuardLayout aiApiKey="sk-abcdef123456789"/>
```

✅ **Do this instead:**
```xml
<LogGuardLayout aiApiKey="${OPENAI_API_KEY}"/>
```

Then set environment variable:
```bash
export OPENAI_API_KEY="sk-..."
java -jar myapp.jar
```

### 2. Limit Sampling in Sensitive Systems

```xml
<!-- Only analyze 5% of logs -->
<LogGuardLayout samplingRate="0.05"/>
```

### 3. Set Appropriate Timeout

```xml
<!-- 2 seconds prevents hanging -->
<LogGuardLayout aiTimeoutMs="2000"/>
```

### 4. Use Threshold Wisely

```xml
<!-- Raise threshold to limit AI calls -->
<LogGuardLayout aiThreshold="7"/>  <!-- Only very high risk -->
```

---

## 🎨 Programmatic Configuration (Java)

```java
import com.logguardai.layout.LogGuardLayout;
import java.nio.charset.Charset;

// Create with all parameters
Layout layout = LogGuardLayout.createLayout(
    Charset.forName("UTF-8"),
    true,                       // aiEnabled
    "openai",                   // aiProvider
    System.getenv("OPENAI_API_KEY"),  // aiApiKey
    "gpt-3.5-turbo",            // aiModel
    5,                          // aiThreshold
    2000L,                      // aiTimeoutMs
    0.1                         // samplingRate (10%)
);

// Add to appender
Appender appender = new ConsoleAppender("Console", layout);
```

---

## 🔍 Environment Variables

### Required (if AI enabled)
- `OPENAI_API_KEY` — Your OpenAI API key

### Optional
- `LOGGUARDAI_THRESHOLD` — Override threshold
- `LOGGUARDAI_SAMPLING` — Override sampling rate

### Example
```bash
export OPENAI_API_KEY="sk-..."
export LOGGUARDAI_SAMPLING="0.05"
java -jar myapp.jar
```

---

## ⚙️ Advanced Configuration

### Custom Timeout per Environment

```xml
<!-- Development: Fast feedback -->
<LogGuardLayout aiTimeoutMs="500"/>

<!-- Staging: Balanced -->
<LogGuardLayout aiTimeoutMs="2000"/>

<!-- Production: Reliability -->
<LogGuardLayout aiTimeoutMs="3000"/>
```

### Cost Optimization

**High-volume systems:**
```xml
<!-- Start with 1% sampling -->
<LogGuardLayout samplingRate="0.01"/>
<!-- Monitor costs, increase as needed -->
```

**Security-critical systems:**
```xml
<!-- Use higher threshold -->
<LogGuardLayout aiThreshold="7"/>  <!-- Only extreme risk -->
```

**Budget-conscious:**
```xml
<!-- Cache-friendly + low sampling -->
<LogGuardLayout samplingRate="0.05" aiModel="gpt-3.5-turbo"/>
```

---

## 🧪 Testing Configuration

```xml
<!-- Non-blocking, fast feedback -->
<LogGuardLayout
    aiEnabled="true"
    aiTimeoutMs="500"
    samplingRate="0.5"/>
```

---

## 📊 Configuration Checklist

- [ ] Charset set appropriately
- [ ] AI enabled/disabled as intended
- [ ] API key loaded from environment (not hardcoded)
- [ ] Timeout set for your network (1-3 seconds typical)
- [ ] Sampling rate optimized for cost/coverage
- [ ] Threshold matches your risk tolerance
- [ ] Model chosen (GPT-3.5 for cost, GPT-4 for quality)

---

## 🔗 Related Documentation

- **Quick Start:** [Getting started in 5 minutes](../versions/v0.2/QUICK_START.md)
- **AI Guide:** [Full AI integration guide](../versions/v0.2/AI_GUIDE.md)
- **Release Notes:** [What changed](../versions/v0.2/RELEASE_NOTES.md)

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)

---

*Last Updated: 2026-04-23 | Configuration applies to v0.2+*

[← Back to Guides](README.md)
