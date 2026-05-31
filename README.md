# LogGuardAI

**Intelligent Log Sanitization for Java** — Automatically detect and mask sensitive data in logs using rule-based analysis and AI.

[![GitHub Stars](https://img.shields.io/github/stars/sanitizeai/logguardai?style=flat-square)](https://github.com/sanitizeai/logguardai)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.sanitizeai/logguardai?style=flat-square)](https://jitpack.io/#sanitizeai/logguardai)
[![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](LICENSE)

---

## 🎯 What is LogGuardAI?

LogGuardAI is a Log4j2 plugin that intercepts log events and intelligently sanitizes them to **prevent accidental exposure of sensitive data** like passwords, API keys, tokens, and personal information.

### The Problem
```
❌ Logs with secrets:
  [LOG] User auth: userId=12345 apiKey=sk-abcdef123456 password=secret
        ↑ Oops! Now this secret is in logs, monitoring dashboards, and backups!
```

### The Solution
```
✅ Logs with LogGuardAI:
  [LOG] User auth: userId=***** apiKey=***** password=*****
        ↑ Secrets automatically masked, structure preserved!
```

---

## ✨ Features

### v0.1: Rule-Based Sanitization (Fast & Reliable)
- 🔐 **20+ Sensitive Keyword Detection** — Automatically detects passwords, tokens, API keys, etc.
- 📊 **Smart Risk Scoring** — Multi-factor analysis (keywords, patterns, entropy)
- ⚡ **Lightning Fast** — <5ms latency, non-blocking design
- 🛡️ **Fail-Safe** — Logging never breaks, even if something fails
- 📝 **Structured Data Support** — Handles key=value, JSON, query strings

### v0.2: AI-Enhanced (Optional, Cost-Effective)
- 🧠 **AI-Powered Sanitization** — OpenAI GPT-based intelligent masking with context
- 💰 **LRU Caching** — 80-95% cache hit rate = **80% cost savings**
- 🏆 **Enhanced Exceptions** — AI explains what went wrong
- 🔍 **Data Classification** — Automatically identify PII vs. sensitive data
- 📊 **Configurable Sampling** — Control AI costs (5%-100%)

### v0.3: Batch Processing (Performance Optimized)
- 📦 **Batch AI Processing** — Process multiple sensitive values together (5x efficiency)
- ⚡ **Async/Non-Blocking** — AI calls never block log threads (<5ms guaranteed)
- 🔄 **Smart Fallbacks** — Rule-based masking if AI fails or times out

- 🐛 **Spring Boot Compatibility** — Fixed Log4j2 plugin registration issues

### v0.4: Multi-Provider AI Support (Enterprise Ready)
- 🤖 **Multi-Provider AI** — OpenAI, Anthropic Claude, or Azure OpenAI
- 🏗️ **Anthropic Claude Integration** — Claude 3 models with Messages API
- ☁️ **Azure OpenAI Integration** — Custom endpoints and deployments
- 🔧 **Provider-Specific Configuration** — Azure endpoints, deployments, API versions
- 🏷️ **Enhanced Model Support** — GPT-4, Claude 3, Azure deployments
- 📊 **Configurable Batch Size** — Tune performance vs. API efficiency
- 🤖 **Multi-Provider Support** — OpenAI, Anthropic Claude, Azure OpenAI
- 📈 **Metrics Extraction** — Convert legacy unstructured logs into rich JSON metrics.
- 🏗️ **Backward Compatible** — Existing configurations work unchanged


### v0.5: Pattern-Based Metrics (Optional)
**Released:** May 27, 2026

#### ✨ New Features
- 📊 **Pattern-Based Metrics** — Define custom regex patterns to extract metrics directly from logs
- 🎯 **User-Defined Patterns** — Create metrics for HTTP requests, FTP transfers, database queries, etc.
- 💾 **Append-Only File** — Metrics stored in Prometheus text format with cumulative history
- ⚙️ **Periodic Flushing** — Configurable flush interval (default: 60 seconds)
- 🔐 **Cardinality Protection** — Configurable limits prevent unbounded metric combinations (default: 10,000)
- 🔌 **Zero Dependencies** — No external monitoring infrastructure required

#### Quick Example (log4j2.xml)
```xml
<LogGuardLayout 
  extractMetrics="true"
  metricsFilePath="logs/metrics.txt"
  metricsFlushIntervalMs="60000"
  metricsMaxCardinality="10000"
  metricsPatterns="http_requests|(GET|POST|DELETE) ([/\\w/]+) (\\d{3})|http_requests_total|method,endpoint,status"/>
```

See `docs/guides/metrics-configuration.md` for full configuration details and examples.

---

## 🚀 Quick Start (5 minutes)

### 1. Add Dependency (Maven)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.logguardai</groupId>
    <artifactId>logguardai</artifactId>
    <version>0.5.0</version>  <!-- Latest: Logs to metrics -->
</dependency>
```

### 2. Configure log4j2.xml
```xml
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

Tip: To enable pattern-based metrics, set `extractMetrics="true"` and configure `metricsFilePath`, `metricsFlushIntervalMs`, `metricsMaxCardinality`, and `metricsPatterns` as shown in the v0.5 section above.

### 3. Done! (Optional: Enable AI with Batch Processing)
```xml
<!-- OpenAI (default) -->
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-3.5-turbo"
    samplingRate="0.1"
    extractMetrics="true"
    batchSize="5"/>

<!-- Anthropic Claude -->
<LogGuardLayout
    aiEnabled="true"
    aiProvider="anthropic"
    aiApiKey="${ANTHROPIC_API_KEY}"
    aiModel="claude-3-sonnet-20240229"
    samplingRate="0.1"
    batchSize="5"/>

<!-- Azure OpenAI -->
<LogGuardLayout
    aiEnabled="true"
    aiProvider="azure-openai"
    aiApiKey="${AZURE_OPENAI_API_KEY}"
    aiModel="gpt-35-turbo"
    azureEndpoint="https://your-resource.openai.azure.com"
    azureDeployment="your-deployment-name"
    azureApiVersion="2023-12-01"
    samplingRate="0.1"
    batchSize="5"/>
```

---

## 📊 Comparison: v0.1 vs v0.2 vs v0.3 vs v0.4 vs v0.5

| Feature | v0.1 | v0.2 | v0.3 | v0.4 | v0.5 |
|---------|------|------|------|------|
| **Rule-Based Masking** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **AI Sanitization** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **LRU Caching** | ❌ | ✅ | ✅ | ✅ | ✅ |
| **Batch Processing** | ❌ | ❌ | ✅ | ✅ | ✅ |
| **Async/Non-Blocking** | ❌ | ❌ | ✅ | ✅ | ✅ |
| **Multi-Provider AI** | ❌ | ❌ | ❌ | ✅ | ✅ |
| **Metrics Extraction**| ❌ | ❌ | ❌ | ✅ | ✅ (pattern-based) |
| **Latency** | <5ms | <5ms (rule-based)<br/>~1500ms (API)<br/><1ms (cached) | <5ms guaranteed | <5ms guaranteed | <5ms guaranteed |
| **Cost/10M logs** | $0 | $95 (no cache)<br/>$19 (with cache) | $15-20 (with batching) | $15-20 (with batching) | $15-25 (depends on metrics config) |
| **Dependencies** | 0 | 0 new | 0 new | 0 new | 0 new |
| **Backward Compatible** | N/A | ✅ 100% | ✅ 100% | ✅ 100% | ✅ 100% |

---

## 💡 Examples

### Basic Usage
```java
Logger logger = LoggerFactory.getLogger("MyApp");

// This will be automatically sanitized by LogGuardAI:
logger.info("User login: userId=12345 token=sk-abcdef123 password=secret");

// Output:
// User login: userId=***** token=***** password=*****
```

### Configuration Profiles

**Development (AI Testing)**
```xml
<LogGuardLayout aiEnabled="true" aiProvider="openai" samplingRate="0.05" aiTimeoutMs="1000" batchSize="3"/>
<!-- Or with Anthropic: aiProvider="anthropic" aiModel="claude-3-haiku-20240307" -->
```

**Production (Balanced)**
```xml
<LogGuardLayout aiEnabled="true" aiProvider="openai" samplingRate="0.1" aiTimeoutMs="2000" batchSize="5"/>
<!-- Or with Azure: aiProvider="azure-openai" azureEndpoint="..." azureDeployment="..." -->
```

**High-Security (Premium)**
```xml
<LogGuardLayout aiEnabled="true" aiProvider="openai" aiModel="gpt-4" samplingRate="1.0" batchSize="10"/>
<!-- Or with Claude: aiProvider="anthropic" aiModel="claude-3-sonnet-20240229" -->
```

**Fast (No AI)**
```xml
<LogGuardLayout aiEnabled="false"/>  <!-- v0.1 behavior -->
```

---

## 📚 Documentation

### Getting Started
- **[Quick Start Guide](docs/versions/v0.2/QUICK_START.md)** — 5-minute setup (with AI examples)
- **[AI Integration Guide](docs/versions/v0.2/AI_GUIDE.md)** — Deep dive into AI features (3000+ words)
- **[Migration Guide](docs/versions/v0.2/MIGRATION.md)** — v0.1 → v0.2 upgrade path

### Configuration & Usage
- **[Configuration Reference](docs/guides/configuration.md)** — All configuration options
- **[Troubleshooting](docs/guides/troubleshooting.md)** — Common issues & solutions
- **[Project Structure](docs/guides/project-structure.md)** — Code organization

### Architecture & API
- **[System Overview](docs/architecture/overview.md)** — How LogGuardAI works
- **[Components](docs/architecture/components.md)** — Detailed component descriptions
- **[AIService API](docs/api/air-service-interface.md)** — Interface reference

### Version Information
- **[CHANGELOG](CHANGELOG.md)** — Complete version history
- **[v0.2.0 Release Notes](docs/versions/v0.2/RELEASE_NOTES.md)** — v0.2 features
- **[v0.1.0 Docs](docs/versions/v0.1/)** — Previous version

### Publishing
- **[JitPack Publishing](docs/guides/jitpack-publishing.md)** — How to publish

---

## 🎯 Use Cases

### E-Commerce Platform
```java
// Transaction logs with credit card & billing info
logger.info("Transaction: user_id={} amount={} card={} crypto_key={}",
    userId, amount, creditCard, encryptionKey);
// Automatically masked! ✅
```

### Authentication Service
```java
// Auth logs with tokens & credentials
logger.info("Auth attempt: username={} password={} jwt_token={}",
    username, password, jwtToken);
// All masked automatically! ✅
```

### API Gateway
```java
// Headers & API keys
logger.info("Incoming request: Authorization={} X-API-Key={} User-Agent={}",
    bearerToken, apiKey, userAgent);
// Headers sanitized intelligently! ✅
```

Also: use `metricsPatterns` to extract request counts, paths and status codes for lightweight in-app metrics collection.

---

## 📊 Performance

### Latency
```
Rule-Based Path:  <5ms    (fast, no external calls)
AI Hit (cached):  <1ms    (in-memory cache)
AI Miss (API):    ~1.5s   (to OpenAI, happens ~20% with 80% cache hit rate)
```

### Cost (10 million logs/month)
```
Without AI:              $0/month   (rule-based only)
With AI (no cache):      $95/month  (5% sampling)
With AI + caching:       $19/month  (5% sampling, 80% cache hits)
Savings:                 80%
```

### Throughput
```
Rule-Based Only:        >50,000 logs/sec
With AI Sampling:       >45,000 logs/sec (minimal impact)
With Caching:           >100,000 logs/sec (cached dominate)
```

---

## 🔒 Security

✅ **Secrets Never Exposed**
- API keys, passwords, tokens, PII automatically masked
- Fail-safe: if LogGuard fails, logging still works (never breaks!)
- Timeout protection: no hanging requests

✅ **API Key Protection**
- API key stored in environment variable, never hardcoded
- No logs sent to external services (caching keeps results locally)

✅ **Non-Blocking Design**
- Independent executor for AI calls
- No thread blocking
- Application never slows down

---

## 🛠️ Build & Test

### Prerequisites
- Java 8+
- Maven 3.6+

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Verify Installation
```bash
ls target/logguardai-0.5.0.jar
```

---

## 📦 Available Versions

| Version | Release Date | Status | What's New |
|---------|--------------|--------|-----------|
| **v0.5.0** | 2026-05-27 | ✅ Latest | Pattern-Based Metrics (user-defined regex metrics) |
| **v0.4.0** | 2026-05-03 | ✅ Stable | Multi-Provider AI Support |
| **v0.3.0** | 2026-05-01 | ✅ Stable | Plugin Registration Fix |
| **v0.2.0** | 2026-04-23 | ✅ Stable | AI + Caching |
| **v0.1.0** | 2026-04-22 | ✅ Stable | Rule-based masking |

→ See [CHANGELOG](CHANGELOG.md) for complete history

---

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Submit a pull request

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Report Issues:** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)
- **Full Documentation:** [docs/README.md](docs/README.md)

---

## 📋 License

MIT License - See [LICENSE](LICENSE) for details

---

## 🚀 Next Steps

1. **Get Started** → [Quick Start Guide](docs/versions/v0.2/QUICK_START.md)
2. **Enable AI** → [AI Integration Guide](docs/versions/v0.2/AI_GUIDE.md)
3. **Configure** → [Configuration Reference](docs/guides/configuration.md)
4. **Troubleshoot** → [Troubleshooting Guide](docs/guides/troubleshooting.md)

---

**LogGuardAI: Secure your logs. Automatically.**

*Built with ❤️ by [sanitizeai](https://github.com/sanitizeai)*
