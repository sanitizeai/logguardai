# LogGuardAI v0.1 - Implementation Guide

## 🚀 Quick Start

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

### Use in Your Project

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.logguardai</groupId>
    <artifactId>logguardai-core</artifactId>
    <version>0.1.0</version>
</dependency>
```

---

## 📋 Configuration

Configure Log4j2 with LogGuardLayout in `log4j2.xml`:

```xml
<Configuration packages="com.logguardai">
    <Appenders>
        <Console name="Console">
            <LogGuardLayout
                aiEnabled="false"
                aiThreshold="5"
                timeoutMs="150"
                samplingRate="0.05"/>
        </Console>
    </Appenders>
    <Root level="info">
        <AppenderRef ref="Console"/>
    </Root>
</Configuration>
```

### Configuration Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `aiEnabled` | `false` | Enable AI-based sanitization (v0.2+) |
| `aiThreshold` | `5` | Risk score threshold for AI sanitization |
| `timeoutMs` | `150` | Max timeout for AI calls (ms) |
| `samplingRate` | `0.05` | Sampling rate for AI calls (0.0-1.0) |
| `charset` | `UTF-8` | Character encoding |

---

## 🧩 Core Components

### 1. **LogTokenizer**
Breaks log messages into tokens:
- Parses `key=value` pairs
- Handles JSON logs
- Supports query string format

```java
LogTokenizer tokenizer = new LogTokenizer();
List<Token> tokens = tokenizer.tokenize("userId=12345 token=abc123");
```

### 2. **RiskScoringEngine**
Scores tokens based on:
- Sensitive keywords (`password`, `token`, `id`, etc.)
- Value characteristics (entropy, length, patterns)
- Returns score 0-10+

```java
RiskScoringEngine engine = new RiskScoringEngine();
engine.scoreTokens(tokens);
// token.getRiskScore() → 0-2 (low), 3-5 (medium), >5 (high)
```

### 3. **DecisionEngine**
Routes based on risk score:
- 0-2: Pass through (no action)
- 3-5: Rule-based masking
- >5: AI sanitization (if enabled)

```java
DecisionEngine engine = new DecisionEngine();
DecisionEngine.SanitizationAction action = engine.decideSanitization(4);
// SanitizationAction.RULE_BASED_MASK
```

### 4. **SanitizationEngine**
Applies rule-based masking:
- Replaces sensitive values with `*****`
- Preserves structure and keys

```java
SanitizationEngine engine = new SanitizationEngine();
String masked = engine.buildMaskedPair("password", "secret");
// "password=*****"
```

### 5. **ExceptionProcessor**
Generates human-friendly insights:
- Maps common exceptions
- Provides likely causes
- Suggests fixes

```java
ExceptionProcessor processor = new ExceptionProcessor();
String insight = processor.generateInsight(throwable);
// "A null reference was used. Cause: object was not initialized..."
```

---

## 📝 Usage Examples

### Basic Logging with Sanitization

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyApp {
    private static final Logger logger = LogManager.getLogger(MyApp.class);

    public static void main(String[] args) {
        // This will be automatically sanitized
        String userId = "839274923749237";
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        
        logger.info("User login: userId={} token={}", userId, token);
        // Output: User login: userId=***** token=*****
    }
}
```

### Exception Logging with Insights

```java
try {
    Object obj = null;
    obj.toString();
} catch (NullPointerException e) {
    logger.error("Operation failed", e);
    // Auto-generated insight:
    // "A null reference was used. Cause: object was not initialized before use..."
}
```

### Structured Logging (JSON)

```java
logger.info("\" {\"userId\":\"123\",\"action\":\"login\",\"apiKey\":\"secret123\"}");
// Tokenized and sanitized automatically
```

---

## 🧪 Testing

Run all tests:
```bash
mvn test
```

Run specific test:
```bash
mvn test -Dtest=RiskScoringTest
```

---

## 📦 v0.1 Features (Complete)

✅ **Tokenizer** - Key=value and JSON parsing  
✅ **Risk Scoring** - Context-aware heuristics  
✅ **Decision Engine** - Risk-based routing  
✅ **Rule-based Masking** - Value replacement  
✅ **Exception Processing** - Common exception insights  
✅ **Log4j2 Integration** - Native plugin  

---

## 🔮 Upcoming Features

### v0.2
- AI-based sanitization interface
- Caching layer for repeated tokens
- Extended exception explanations

### v0.3
- Async processing pipeline
- Advanced sampling strategies

### v1.0
- Spring Boot starter
- Production monitoring
- Configuration profiles

---

## ⚡ Performance

- **Non-AI path**: < 5ms latency
- **Non-blocking**: Always fail-safe
- **Memory efficient**: Minimal token storage

---

## 🐛 Troubleshooting

### Logs not being sanitized?
- Check `log4j2.xml` uses `LogGuardLayout`
- Verify `packages="com.logguardai"` in Configuration

### Performance issues?
- Reduce `samplingRate` for AI calls (v0.2+)
- Increase `timeoutMs` threshold
- Monitor heap usage

### Missing exception insights?
- Exception must be pass as 2nd parameter: `logger.error("msg", throwable)`
- Check exception type is mapped (see ExceptionProcessor.java)

---

## 📄 License

MIT License - See LICENSE file

---

**Questions?** Open an issue on GitHub!
