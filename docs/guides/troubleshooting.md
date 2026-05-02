# Troubleshooting Guide

Common issues and how to fix them.

**Applies To:** All versions

---

## 🔴 Application Won't Start

### "LogGuardLayout class not found"

**Symptom:**
```
ClassNotFoundException: com.logguardai.layout.LogGuardLayout
```

**Causes:**
- Dependency not added to pom.xml
- JAR not on classpath
- Wrong version specified

**Solutions:**

1. **Verify Maven dependency:**
```xml
<dependency>
    <groupId>com.github.sanitizeai</groupId>
    <artifactId>logguardai</artifactId>
    <version>v0.3.0</version>  <!-- Use correct version -->
</dependency>
```

2. **Add JitPack repository:**
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

3. **Rebuild:**
```bash
mvn clean compile
```

---

### "No Log4j2 configuration found"

**Symptom:**
```
ERROR StatusLogger No config found for logguardai
```

**Cause:** log4j2.xml missing or misconfigured

**Solution:**

Create `src/main/resources/log4j2.xml`:
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

---

## 🟡 AI Features Not Working (v0.2)

### "AI disabled" or no API calls

**Symptom:**
- Logs not being processed by AI
- No external API calls visible

**Causes:**
- `aiEnabled="false"` (default)
- API key not set
- Sampling rate = 0

**Solutions:**

1. **Enable AI:**
```xml
<LogGuardLayout aiEnabled="true"/>
```

2. **Set API key:**
```xml
<LogGuardLayout aiApiKey="${OPENAI_API_KEY}"/>
```

And environment variable:
```bash
export OPENAI_API_KEY="sk-..."
```

3. **Check sampling rate:**
```xml
<!-- Not being sampled? Increase rate -->
<LogGuardLayout samplingRate="0.5"/>  <!-- 50% -->
```

---

### "OpenAI API Error: 401 Unauthorized"

**Symptom:**
```
ERROR OpenAI API error: 401: Incorrect API key provided
```

**Causes:**
- Invalid API key
- Expired key
- Key not set
- Typo in environment variable

**Solutions:**

1. **Verify API key:**
```bash
echo $OPENAI_API_KEY  # Should show sk-...
```

2. **Get valid key from:**
```
https://platform.openai.com/account/api-keys
```

3. **Set correctly:**
```bash
export OPENAI_API_KEY="sk-YOUR-KEY-HERE"
java -jar myapp.jar
```

4. **Test with curl:**
```bash
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

---

### "OpenAI API Error: 429 Rate Limit"

**Symptom:**
```
ERROR OpenAI API error: 429: Rate limit exceeded
```

**Causes:**
- Too many API calls (sampling rate too high)
- Hitting OpenAI organization limit
- Free tier quota exceeded

**Solutions:**

1. **Lower sampling rate:**
```xml
<!-- From 100% to 10% -->
<LogGuardLayout samplingRate="0.1"/>
```

2. **Use GPT-3.5 (cheaper):**
```xml
<LogGuardLayout aiModel="gpt-3.5-turbo"/>
```

3. **Upgrade OpenAI account** at https://platform.openai.com/account/billing

4. **Monitor usage:**
```
https://platform.openai.com/account/usage/overview
```

---

### "Timeout calling AI service"

**Symptom:**
```
WARN Timeout calling AI service after 2000ms, falling back to rule-based
```

**Causes:**
- Network is slow
- OpenAI is slow
- Timeout too aggressive

**Solutions:**

1. **Increase timeout:**
```xml
<!-- From 2 seconds to 3 seconds -->
<LogGuardLayout aiTimeoutMs="3000"/>
```

2. **Check network:**
```bash
# Test latency to OpenAI
curl -w "Total time: %{time_total}s\n" https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

3. **Lower sampling rate** (fewer concurrent calls):
```xml
<LogGuardLayout samplingRate="0.05"/>
```

---

## 🟠 Performance Issues

### "Logs are slow"

**Symptoms:**
- Application running slow
- High CPU usage
- Logs appear delayed

**Typical Causes (in order):**
1. High sampling rate with slow network
2. AI threshold too low (too many API calls)
3. Timeout set too high
4. Cache capacity too small

**Solutions:**

1. **Lower sampling rate:**
```xml
<LogGuardLayout samplingRate="0.05"/>  <!-- Reduce from 0.1 -->
```

2. **Raise AI threshold:**
```xml
<LogGuardLayout aiThreshold="7"/>  <!-- Only very risky logs -->
```

3. **Reduce timeout:**
```xml
<LogGuardLayout aiTimeoutMs="1000"/>  <!-- Fail faster -->
```

4. **Check cache settings:**
- Cache should hit 80%+ of requests
- If not, increase cache capacity

---

### "High memory usage"

**Symptoms:**
- Memory keeps growing
- OOM errors
- Garbage collection pauses

**Causes:**
- Cache capacity too large
- Too many cached entries
- Log accumulation

**Solutions:**

1. **Monitor cache:**
```java
if (cachedAiService instanceof CachedAIService) {
    CacheStats stats = ((CachedAIService) cachedAiService).getCacheStats();
    System.out.println(stats);  // Check hit rate
}
```

2. **Reduce cache size:**
```
Default: 1000 entries × ~1KB = 1MB
Monitor actual usage, adjust if needed
```

3. **Check log volume:**
```
If logging >100K events/sec, sample more aggressively
```

---

## 🟢 Logs Not Being Sanitized

### "Values not masked"

**Symptom:**
```
Logs show passwords/tokens in plaintext
```

**Causes:**
- LogGuard not applied to appender
- Risk scoring not detecting secrets
- Masking disabled

**Solutions:**

1. **Verify configuration:**
```xml
<Appenders>
    <Console name="Console">
        <LogGuardLayout/>  <!-- Must have this -->
    </Console>
</Appenders>
```

2. **Check risk scoring:**
- Verify sensitive keywords are detected
- Test with known keywords (password, apikey, token, etc.)

3. **Lower threshold if needed:**
```xml
<LogGuardLayout aiThreshold="3"/>  <!-- More aggressive -->
```

---

### "Only some logs sanitized"

**Symptom:**
```
Some values masked, others not
```

**Cause:** Sampling rate is preventing some logs from AI processing

**Solution:**

If using v0.2 AI:
```xml
<!-- Increase sampling rate -->
<LogGuardLayout samplingRate="0.5"/>  <!-- 50% instead of 10% -->
```

Or switch to rule-based only:
```xml
<LogGuardLayout aiEnabled="false"/>  <!-- 100% coverage -->
```

---

## 🔵 Migration Issues (v0.1 → v0.2)

### "Config incompatible with v0.2"

**Symptom:**
```
ERROR Could not process XML: Invalid attribute
```

**Cause:** Using v0.2 attributes in v0.1 config parser

**Solution:**

Only add new attributes **if using v0.2**:
```xml
<!-- v0.1 compatible in v0.2 -->
<LogGuardLayout/>

<!-- Can add v0.2 attributes -->
<LogGuardLayout aiEnabled="true"/>
```

---

## 📊 Diagnostic Checklist

When troubleshooting, check:

- [ ] **Dependency:** Is logguardai in pom.xml?
- [ ] **Repository:** Is JitPack repository configured?
- [ ] **Configuration:** Is log4j2.xml in resources?
- [ ] **Syntax:** Is XML valid?
- [ ] **Permissions:** Can application access environment variables?
- [ ] **Network:** Can application reach OpenAI (port 443)?
- [ ] **API Key:** Is OPENAI_API_KEY set and valid?
- [ ] **Timeout:** Is 2-3 seconds reasonable for your network?
- [ ] **Sampling:** Is rate >0 (not disabled)?
- [ ] **Logs:** Are sensitive values actually in logs being processed?

---

## 🧪 Testing & Validation

### Verify Installation

```bash
# Check JAR loaded
jar tf ~/.m2/repository/com/github/sanitizeai/logguardai/v0.3.0/logguardai-v0.3.0.jar | grep LogGuardLayout

# Should show: com/logguardai/layout/LogGuardLayout.class
```

### Verify Configuration

```java
// Test minimal logging
Logger logger = LoggerFactory.getLogger("test");
logger.info("password=secret123");  // Should mask
logger.info("userId=12345");        // Should mask
logger.info("message=hello");       // Should pass
```

### Verify AI (v0.2)

```bash
# Check API key
echo $OPENAI_API_KEY

# Should show: sk-...

# Test with curl
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"

# Should show list of models
```

---

## 📞 Getting Help

If basic troubleshooting doesn't work:

1. **Check GitHub Issues:** [logguardai/issues](https://github.com/sanitizeai/logguardai/issues)
2. **Ask in Discussions:** [logguardai/discussions](https://github.com/sanitizeai/logguardai/discussions)
3. **Create new issue** with:
   - LogGuardAI version
   - Error message
   - Configuration (with API key redacted)
   - Steps to reproduce

---

## 🔗 Related Documentation

- **Configuration:** [All config options](configuration.md)
- **Quick Start:** [Get started](../versions/v0.2/QUICK_START.md)
- **AI Guide:** [AI features deep dive](../versions/v0.2/AI_GUIDE.md)

---

*Last Updated: 2026-04-23*

[← Back to Guides](README.md)
