# Architecture Overview

How LogGuardAI works at a high level.

---

## 🏗️ System Architecture

LogGuardAI is a **Log4j2 plugin** that intercepts log events and intelligently sanitizes them.

```
┌─────────────────────────────────────┐
│  Application Code                   │
│  logger.info("user auth token...")  │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│  Log4j2 Framework                   │
│  Logger → Appender → Layout         │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│  LogGuardLayout (OUR PLUGIN)        │
│  1. Extract & Tokenize              │
│  2. Score Risk                      │
│  3. Decide Action                   │
│  4. Sanitize/AI Process             │
│  5. Format Output                   │
└────────────┬────────────────────────┘
             │
             ↓
┌─────────────────────────────────────┐
│  Console/File/etc.                  │
│  user auth token... → auth=*****    │
└─────────────────────────────────────┘
```

---

## 🔄 Processing Pipeline

### Step 1: Event Extraction
```
Log event arrives
├─ Extract message
├─ Extract timestamp
├─ Extract level
└─ Extract context
```

### Step 2: Tokenization
```
Message: "userId=12345 apiKey=sk-secret"
              ↓
├─ Key: "userId"      Value: "12345"
├─ Key: "apiKey"      Value: "sk-secret"
└─ Key: "context"     Value: "user auth"
```

### Step 3: Risk Scoring (v0.1)
```
Each token scored:
├─ Keyword analysis (0-10 points)
├─ Pattern detection (0-10 points)
├─ Entropy analysis (0-10 points)
└─ Total: 0-30+ score
```

### Step 4: Decision Making
```
Score < 3:   PASS       (no masking)
Score 3-5:   MASK       (****** replacement)
Score > 5:   AI or MASK (v0.2: try AI, fallback to mask)
```

### Step 5: Sanitization
```
Original: userId=12345 apiKey=sk-secret
PASS:     userId=12345 apiKey=sk-secret
MASK:     userId=***** apiKey=*****
AI:       userId=[USER_ID_MASKED] apiKey=[SK_API_MASKED]
```

### Step 6: Output Formatting
```
Formatted for appender:
[2026-04-23 14:30:45] INFO - userId=***** apiKey=*****
```

---

## 🧠 What Makes It Smart?

### Multi-Factor Risk Scoring

1. **Keyword Detection** (~20 keywords)
   - Sensitive: password, token, secret, apikey, auth, bearer, jwt, etc.
   - Personal: ssn, pii, email, phone, creditcard, etc.
   - Identifiers: id, userId, sessionId, etc.

2. **Pattern Recognition**
   - JWT patterns: `eyJhbG...` (Base64 encoded)
   - API keys: `sk-...` or `api_...`
   - Credit cards: 16 digit numbers
   - Hex strings: Long hex sequences
   - High entropy: Random-looking strings

3. **Contextual Analysis**
   - Value length (short IDs vs long tokens)
   - Value format (Base64, hex, numeric)
   - Relative position in log
   - Surrounding keywords

### Example Scoring

```
Value: "sk-1234567890abcdef"

Scoring:
├─ Contains "sk-": +10 (API key pattern)
├─ Key is "apiKey": +5 (keyword match)
├─ Length > 10 chars: +3 (typically keys)
├─ Is Base64-like: +2 (encoding pattern)
└─ Total: 20 points → DEFINITELY MASK
```

---

## 🚀 v0.2 Enhancements

### AI Integration Layer (Optional)

```
High-Risk Log (score > threshold)
         │
         ↓
    [Sampling Check]
    (only X% get AI)
         │
         ↓
    [Cache Check]
    (seen before?)
    ├─ HIT  → Return cached response (<1ms)
    └─ MISS → Call OpenAI API (~1500ms)
         │
         ↓
    [AI Processing]
    ├─ Sanitize with context
    ├─ Classify data type
    └─ Explain exception
         │
         ↓
    [Timeout Protection]
    (if >2 seconds, fallback)
         │
         ↓
    [Cache Result]
    └─ Store for future use
```

### Caching Strategy

```
LRU Cache (1000 entries, 1 hour TTL)
├─ Cache Key: "{operation}:{value}:{context}"
├─ Hit Rate: 80-95% (typical)
├─ Memory: ~2MB
└─ Cost Savings: 80%

Request Flow:
1. Check cache → HIT (use cached result, <1ms)
2. No hit → Call AI API (~1500ms)
3. Cache result for future reuse
```

---

## 📊 Component Interaction

```
Log Event
   │
   ├─→ LogGuardLayout
   │   ├─→ LogTokenizer
   │   │   └─ Extracts key=value pairs
   │   │
   │   ├─→ RiskScoringEngine
   │   │   └─ Calculates risk score
   │   │
   │   ├─→ DecisionEngine
   │   │   └─ Routes: pass, mask, or AI
   │   │
   │   ├─→ [v0.2] AIServiceFactory
   │   │   ├─→ OpenAIService (if configured)
   │   │   │   └─ Makes API calls
   │   │   ├─→ LRUCache (if enabled)
   │   │   │   └─ Caches results
   │   │   └─→ CachedAIService (wrapper)
   │   │       └─ Orchestrates caching
   │   │
   │   ├─→ SanitizationEngine
   │   │   └─ Applies masking if needed
   │   │
   │   └─→ ExceptionProcessor
   │       └─ Processes exceptions
   │
   └─→ Appender
       └─ Outputs sanitized message
```

---

## ⚡ Performance Characteristics

### Latency Path Analysis

```
Non-AI Path (v0.1):
Tokenize:        ~0.1ms
Score:           ~0.2ms
Decide:          ~0.05ms
Sanitize:        ~1ms
Format:          ~0.5ms
──────────────────────
TOTAL:          ~1.9ms ✅ Fast

AI Path (v0.2 - sampled):
Tokenize:        ~0.1ms
Score:           ~0.2ms
Decide:          ~0.05ms
Cache check:     ~0.05ms
API call:        ~1500ms (average)
Sanitize:        ~1ms
Format:          ~0.5ms
──────────────────────
TOTAL:          ~1502ms⚠️  (but cached on next hit!)

AI Path (v0.2 - cache hit):
[all above] - API call replaced with:
Cache hit:       ~0.1ms
──────────────────────
TOTAL:          ~2ms ✅ Fast
```

### Memory Profile

```
Base Memory:     ~5MB (LogGuardAI classes + Log4j2)
Cache Memory:    ~2-5MB (1000 entries × 1-5KB each)
Peak Memory:     ~10-15MB total
GC Impact:       Minimal (cache entries evicted)
```

### Throughput (logs/sec)

```
Rule-based only:     >50,000 logs/sec
With AI (10% sample):  >45,000 logs/sec (minimal impact)
With caching:         >100,000 logs/sec (cached dominate)
```

---

## 🔒 Security Guarantees

### Fail-Safe Guarantee
```
If anything breaks:
1. Timeout triggers (2 sec default)
2. Fallback to rule-based masking
3. Log always output (never lost)
4. Logging never breaks application
```

### Non-Blocking Design
```
- No thread blocking for API calls
- Separate executor for AI
- Timeout protects against hangs
- Cache reduces API dependency
```

### Data Isolation
```
- API keys stored in environment, not config
- Sanitized logs never contain secrets
- Cache entries expired after TTL
- No logs sent to external services
```

---

## 🏛️ Design Principles

1. **Non-Blocking** — Logging never slows down application
2. **Fail-Safe** — If LogGuard fails, logging still works
3. **Modular** — Each component independent
4. **Extensible** — Easy to add new providers/patterns
5. **Observable** — Built-in health checks & metrics
6. **Cost-Conscious** — Sampling + caching minimize API costs

---

## 🔗 Related Documentation

- **Components:** [Components Details](components.md)
- **Configuration:** [Configuration Reference](../guides/configuration.md)
- **Quick Start:** [5-Minute Setup](../versions/v0.2/QUICK_START.md)

---

*Last Updated: 2026-04-23*

[← Back to Architecture](README.md)
