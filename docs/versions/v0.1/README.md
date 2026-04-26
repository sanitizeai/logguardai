# LogGuardAI v0.1.0 Documentation

Welcome to v0.1.0! This is the original release with core rule-based sanitization.

**Status:** ✅ Stable | **Released:** April 22, 2026 | **Current Alternative:** [v0.2.0 (AI-Powered)](../v0.2/)

---

## 📚 Documentation Files

| Document | Purpose | Time |
|----------|---------|------|
| **[Implementation Details](IMPLEMENTATION.md)** | Technical architecture | 15 min |

---

## 🎯 Quick Navigation

### **What does v0.1 do?**
- Fast rule-based log sanitization
- Detects 20+ sensitive keywords
- Masks values with `*****`
- Non-blocking (<5ms)
- Zero dependencies beyond Log4j2

### **Should I use v0.1 or v0.2?**
- **v0.1:** Fast, simple, rule-based
- **v0.2:** AI-powered, smarter masking, caching, 80% cost savings

→ See [Comparison](../v0.2/MIGRATION.md#-what-changed)

---

## ✨ v0.1 Features

### Core Capabilities
- 🔐 **Rule-Based Sanitization** — Keyword + pattern detection
- 📝 **Smart Tokenization** — key=value pairs, JSON, query strings
- 📊 **Risk Scoring** — Multi-factor assessment
- 🚦 **Decision Engine** — Pass → Mask → AI (v0.2+)
- ⚠️ **Exception Mapping** — Insights for 10+ exceptions
- ⚡ **Non-blocking** — <5ms latency

### Sensitive Patterns Detected
- Identifiers: `id`, `token`, `password`, `secret`, `auth`, `apikey`
- Finance: `creditcard`, `cc`, `cvv`, `pin`
- Personal: `ssn`, `pii`, `email`, `phone`, `name`
- Authentication: `bearer`, `jwt`, `session`
- Entropy: Hex strings, Base64, numeric IDs

### Exceptions Known
- NullPointerException
- IllegalArgumentException
- IndexOutOfBoundsException
- NumberFormatException
- IllegalStateException
- ClassCastException
- UnsupportedOperationException
- OutOfMemoryError
- StackOverflowError
- TimeoutException

---

## 📊 Key Facts

| Aspect | Details |
|--------|---------|
| **Latency** | <5ms (non-blocking) |
| **Cost** | $0 (no external APIs) |
| **Dependencies** | Zero new (uses Log4j2) |
| **Tests** | 29 (all passing) |
| **Reliability** | Fail-safe, always works |

---

## 🚀 Quick Start

### 1. Add Dependency (Maven)

```xml
<dependency>
    <groupId>com.github.sanitizeai</groupId>
    <artifactId>logguardai</artifactId>
    <version>v0.1.0</version>
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

### 3. Done!

Logs are now automatically sanitized:

```
Input:   apiKey=sk-abcdef123456, userId=12345
Output:  apiKey=*****, userId=*****
```

---

## 💡 Configuration

### Minimal (Defaults)
```xml
<LogGuardLayout/>
```

### With Options
```xml
<LogGuardLayout charset="UTF-8"/>
```

---

## 📖 Contents

See [Implementation Details](IMPLEMENTATION.md) for:
- Complete architecture overview
- Component descriptions
- Testing coverage
- Build information

---

## 🔄 What's Different in v0.2?

v0.2 adds:
- ✅ AI-powered sanitization (opt-in)
- ✅ LRU caching (80% cost savings)
- ✅ Enhanced exception explanation
- ✅ Data classification
- ✅ Sampling mechanism

**But:** v0.1 behavior is still available in v0.2 with `aiEnabled="false"`

See [v0.1 → v0.2 Migration](../v0.2/MIGRATION.md)

---

## 📈 Performance

```
Operation              Time      Cost
─────────────────────────────────────────
Non-AI path           <5ms      $0
Rule-based masking    ~1ms      $0
Exception processing  ~2ms      $0
Total per log        <5ms      $0
```

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)
- **Docs?** [Full Documentation Index](../../docs/README.md)

---

## 🎓 Learning Resources

- [Implementation Details](IMPLEMENTATION.md) — Technical deep dive
- [GitHub Repository](https://github.com/sanitizeai/logguardai)
- [v0.2 with AI](../v0.2/) — Next version

---

**v0.1.0 is stable and production-ready. Perfect for rule-based masking!**

[← Back to All Versions](../../docs/README.md)
