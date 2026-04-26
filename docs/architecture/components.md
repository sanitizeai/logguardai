# Component Details

Detailed descriptions of LogGuardAI components.

---

## 📦 v0.1 Core Components

### LogGuardLayout (Main Plugin)
**Package:** `com.logguardai.layout`

Orchestrates the entire sanitization pipeline.

**Key Methods:**
- `toSerializable(LogEvent)` — Main entry point
- `sanitizeMessage(String)` — Coordinates sanitization
- `processException(Throwable)` — Enhances exceptions
- `createLayout()` — Factory method

**Responsibilities:**
- Receives log events
- Extracts message and exception
- Routes through pipeline
- Formats final output

---

### LogTokenizer
**Package:** `com.logguardai.tokenizer`

Parses structured log data into tokens.

**Input Types:**
- Key=value pairs: `userId=12345 apiKey=sk-secret`
- JSON: `{"userId": 12345, "apiKey": "sk-secret"}`
- Query strings: `?userId=12345&apiKey=sk-secret`

**Output:**
```java
List<Token> tokens = [
  Token("userId", "12345"),
  Token("apiKey", "sk-secret")
]
```

**Key Methods:**
- `tokenize(String)` → List of tokens
- Handles nested objects
- Supports multiple formats

---

### RiskScoringEngine
**Package:** `com.logguardai.scoring`

Assesses how "risky" each token is.

**Scoring Factors:**
1. **Keyword Match** (0-10 points)
   - Contains: password, token, secret, apikey, jwt, bearer, etc.
   - Weights: Higher for obviously sensitive keywords

2. **Pattern Detection** (0-10 points)
   - JWT patterns: Base64-encoded structure
   - API keys: `sk-`, `api_` prefixes
   - Credit cards: 16 consecutive digits
   - Hex strings: Long sequences of hex
   - High entropy: Looks random

3. **Context Analysis** (0-10 points)
   - Value length (long → more likely secret)
   - Format detection (Base64, hex, etc.)
   - Position in log

**Total Score:** 0-30+ (higher = more risky)

**Key Methods:**
- `scoreToken(Token)` → int (0-30+)
- `scoreTokens(List<Token>)` → List with scores

---

### DecisionEngine
**Package:** `com.logguardai.scoring`

Routes tokens based on risk score.

**Decision Logic:**
```
Score 0-2:    PASS       (don't mask)
Score 3-5:    MASK       (replace with *****)
Score 6+:     AI_MASK    (use AI in v0.2, or mask in v0.1)
```

**Key Methods:**
- `decideSanitization(int score)` → SanitizationAction
- Returns: PASS, MASK, or AI_MASK

---

### SanitizationEngine
**Package:** `com.logguardai.sanitizer`

Applies masking to high-risk values.

**Approach:**
- Preserves key names
- Replaces values with `*****`
- Maintains log structure

**Example:**
```
Input:  "userId=12345 apiKey=sk-secret"
Output: "userId=***** apiKey=*****"
```

**Key Methods:**
- `maskValue(String)` → String (*****)
- `maskLogMessage(String, List)` → masked message

---

### ExceptionProcessor
**Package:** `com.logguardai.exception`

Generates insights for common exceptions.

**Supported Exceptions:**
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

**Example:**
```
Exception: NullPointerException
Insight:   "A null reference was accessed. 
            Check if object is null before use."
```

**Key Methods:**
- `generateInsight(Throwable)` → String
- Extracts stack trace context
- Maps to predefined insights

---

## 📦 v0.2 AI Components

### AIService (Interface)
**Package:** `com.logguardai.ai`

Contract for all AI implementations.

**Methods:**
```java
// Sanitize a value with context
String sanitize(String value, String context);

// Explain an exception
String explainException(String type, String msg, String stackTrace);

// Classify data type
String classifyData(String value, String context);

// Check if service is healthy
boolean isHealthy();

// Get service name
String getServiceName();
```

**Implementations:**
- OpenAIService (native client)
- NoOpAIService (fallback)
- Custom (user-provided)

---

### OpenAIService
**Package:** `com.logguardai.client`

Native HTTP client for OpenAI GPT API.

**Features:**
- No SDK dependency (uses Java HTTP APIs)
- Concurrent execution (ExecutorService)
- 2-second timeout protection
- Auto health-checking
- Three AI capabilities

**Supported Models:**
- `gpt-3.5-turbo` (faster, cheaper)
- `gpt-4` (slower, smarter)

**Key Methods:**
- `sanitize()` — AI-based masking
- `explainException()` — Enhanced insights
- `classifyData()` — PII/sensitive detection
- `healthCheck()` — API connectivity

---

### LRUCache
**Package:** `com.logguardai.cache`

Thread-safe in-memory cache with TTL.

**Features:**
- Generic type parameters <K, V>
- Configurable capacity (default: 1000)
- TTL-based expiration (default: 1 hour)
- LRU eviction policy
- ReentrantReadWriteLock for thread safety
- Cache statistics

**Configuration:**
```java
LRUCache<String, String> cache = 
  new LRUCache<>(1000, 3600000L);  // 1000 entries, 1 hour TTL
```

**Key Methods:**
- `get(K)` → V (returns null if not found)
- `put(K, V)` → void
- `remove(K)` → void
- `clear()` → void
- `getStats()` → CacheStats

**Performance:**
- Get/Put: O(1) average
- Memory: ~1-2KB per entry
- Hit rate: 80-95% typical

---

### CachedAIService
**Package:** `com.logguardai.client`

Decorator wrapper around AIService for caching.

**Strategy:**
- Intercepts calls before hitting API
- Cache key: `"{operation}:{value}:{context}"`
- Returns cached result if available
- Calls wrapped service on miss
- Caches result for future use

**Example:**
```
Request 1: "sanitize(apiKey, password)" → API call (1500ms)
Request 2: "sanitize(apiKey, password)" → Cache hit (<1ms)
```

**Cache Hit Rate Improvement:**
- Without caching: $95/month
- With caching (80% hit): $19/month
- Savings: **80%**

---

### AIServiceFactory
**Package:** `com.logguardai.client`

Factory pattern for creating AI services.

**Responsibilities:**
- Detect configured provider
- Validate configuration
- Create appropriate service
- Apply caching wrapper
- Return ready-to-use service

**Logic:**
```
1. Check if AI enabled
2. Detect provider (openai, etc.)
3. Validate API key present
4. Create base service (OpenAI/NoOp)
5. Wrap with caching if desired
6. Return ready service
```

**Key Methods:**
- `createService(AIConfig)` → AIService
- `withCaching(AIService, capacity, ttl)` → CachedAIService

---

### NoOpAIService
**Package:** `com.logguardai.client`

No-op fallback implementation.

**Behavior:**
- Returns input unchanged for `sanitize()`
- Returns safe message for `explainException()`
- Returns "unknown" for `classifyData()`
- Always returns true for `isHealthy()`

**Use Cases:**
- AI disabled (`aiEnabled=false`)
- API not configured
- Testing without API
- Graceful degradation

---

### AIConfig
**Package:** `com.logguardai.ai`

Configuration container for AI service.

**Properties:**
- `apiKey` — API key for provider
- `apiProvider` — Which provider (e.g., "openai")
- `model` — Model name (e.g., "gpt-3.5-turbo")
- `maxTokens` — Max response tokens (default: 150)
- `temperature` — Sampling temperature (default: 0.3)
- `timeoutMs` — API timeout (default: 2000)
- `retryOnTimeout` — Retry if timeout (default: true)
- `maxRetries` — Max retry attempts (default: 3)

**Validation:**
- `isConfigured()` — Checks if ready to use

---

### AIServiceException
**Package:** `com.logguardai.ai`

Specialized exception for AI service errors.

**Typical Cases:**
- API authentication failure
- Network timeout
- Malformed response
- Rate limit exceeded
- Configuration error

**Error Handling:**
- Caught by LogGuardLayout
- Triggers fallback to rule-based
- Never breaks logging

---

## 🔄 Component Interaction Flow

```
LogGuardLayout
    ├─→ Extract message/exception
    │
    ├─→ LogTokenizer
    │   └─ Parse into tokens
    │
    ├─→ RiskScoringEngine
    │   └─ Score each token
    │
    ├─→ DecisionEngine
    │   └─ Decide: pass, mask, or AI
    │
    ├─→ [if AI selected] AIServiceFactory
    │   ├─→ Check cache (CachedAIService)
    │   │   ├─ HIT: return cached
    │   │   └─ MISS: call OpenAIService
    │   │       └─ Can timeout (uses LRUCache fallback)
    │   └─ Return AI result or fallback
    │
    ├─→ SanitizationEngine
    │   └─ Apply masking if needed
    │
    ├─→ ExceptionProcessor
    │   └─ Process if exception present
    │
    └─→ Format and output
```

---

## 📊 Communication Protocol

### Token Flow
```java
Token token = new Token("apiKey", "sk-secret");
token.setRiskScore(20);
// Risk score calculated and set during processing
```

### Decision Types
```java
enum SanitizationAction {
    PASS,      // Don't mask
    MASK,      // Use rule-based masking
    AI_MASK    // Use AI (v0.2+)
}
```

### AI Request/Response
```java
// Request
AIService service = factory.createService(config);
String sanitized = service.sanitize("sk-secret", "apiKey");

// Response
"[SK_API_MASKED]"  // AI-generated sanitization
```

---

## 🔗 Related Documentation

- **Overview:** [System Overview](overview.md)
- **Architecture:** [Design Principles](overview.md#-design-principles)
- **Configuration:** [All config options](../guides/configuration.md)
- **Quick Start:** [5-minute setup](../versions/v0.2/QUICK_START.md)

---

*Last Updated: 2026-04-23*

[← Back to Architecture](README.md)
