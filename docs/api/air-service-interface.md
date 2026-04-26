# AIService Interface Reference

Complete API reference for the AIService interface.

---

## 📋 Interface Definition

```java
package com.logguardai.ai;

public interface AIService {
    /**
     * Sanitize a sensitive value using AI.
     * 
     * @param value The value to sanitize (e.g., "sk-secret123")
     * @param context The context (e.g., key name: "apiKey")
     * @return Sanitized representation (e.g., "[SK_API_MASKED]")
     * @throws AIServiceException if AI service fails
     */
    String sanitize(String value, String context) throws AIServiceException;

    /**
     * Generate explanation for an exception.
     * 
     * @param type Exception type (e.g., "NullPointerException")
     * @param message Exception message
     * @param stackTrace Stack trace string
     * @return Human-readable explanation
     * @throws AIServiceException if AI service fails
     */
    String explainException(String type, String message, String stackTrace) 
        throws AIServiceException;

    /**
     * Classify data to determine sensitivity level.
     * 
     * @param value The value to classify
     * @param context Additional context
     * @return Classification ("pii", "sensitive", "public", "unknown")
     * @throws AIServiceException if AI service fails
     */
    String classifyData(String value, String context) throws AIServiceException;

    /**
     * Check if AI service is healthy and reachable.
     * 
     * @return true if service is available, false otherwise
     */
    boolean isHealthy();

    /**
     * Get the name of this AI service.
     * 
     * @return Service name (e.g., "OpenAI", "NoOp")
     */
    String getServiceName();
}
```

---

## 🔧 Method Details

### sanitize(String value, String context)

**Purpose:** Intelligently mask a sensitive value.

**Parameters:**
- `value` — The sensitive value to mask
  - Examples: "sk-secret123", "Bearer token123", "12345"
- `context` — The context/key name
  - Examples: "apiKey", "token", "userId"

**Returns:** Sanitized representation
- Example: "sk-secret123" → "[SK_API_MASKED]"
- Example: "Bearer token123" → "[BEARER_TOKEN_MASKED]"

**Throws:** `AIServiceException` if API fails

**Example:**
```java
AIService service = factory.createService(config);
String sanitized = service.sanitize("sk-secret123", "apiKey");
// Returns: "[SK_API_MASKED]"
```

---

### explainException(String type, String message, String stackTrace)

**Purpose:** Generate human-readable explanation for an exception.

**Parameters:**
- `type` — Exception class name
  - Examples: "NullPointerException", "OutOfMemoryError"
- `message` — Exception message
  - Example: "Cannot invoke method on null object"
- `stackTrace` — Stack trace string (can be truncated)
  - Shows call chain

**Returns:** Explanation string
- Example: "A null reference was accessed. Ensure the object is not null before using it."

**Throws:** `AIServiceException` if API fails

**Example:**
```java
String explanation = service.explainException(
    "NullPointerException",
    "Cannot invoke method on null object",
    "at com.app.UserService.findUser(UserService.java:45)"
);
// Returns: "A null reference was accessed..."
```

---

### classifyData(String value, String context)

**Purpose:** Classify data to identify sensitivity level.

**Parameters:**
- `value` — The data to classify
- `context` — Additional context

**Returns:** Classification string
- `"pii"` — Personally identifiable information (SSN, email, etc.)
- `"sensitive"` — Sensitive business data (tokens, keys, etc.)
- `"public"` — Non-sensitive, safe to log
- `"unknown"` — Could not determine

**Throws:** `AIServiceException` if API fails

**Example:**
```java
String classification = service.classifyData("john@example.com", "email");
// Returns: "pii"

String classification = service.classifyData("Hello World", "message");
// Returns: "public"
```

---

### isHealthy()

**Purpose:** Check if AI service is available.

**Returns:** 
- `true` if service is reachable and working
- `false` if service is unavailable

**Never throws:** Returns false on error, never throws

**Example:**
```java
if (service.isHealthy()) {
    // Safe to use service
    String result = service.sanitize(...);
} else {
    // Service unavailable, use fallback
    logger.warn("AI service unavailable, using rule-based masking");
}
```

**Use Cases:**
- Startup health checks
- Monitoring & alerting
- Connection validation

---

### getServiceName()

**Purpose:** Get friendly name of the service.

**Returns:** Service name string
- Examples: "OpenAI", "NoOp", "Custom"

**Never throws:** Always returns valid string

**Example:**
```java
String name = service.getServiceName();
System.out.println("Using: " + name);  // "Using: OpenAI"
```

---

## 🛠️ Implementation Examples

### NoOpAIService (Fallback)

```java
public class NoOpAIService implements AIService {
    @Override
    public String sanitize(String value, String context) {
        return value;  // Return unchanged
    }

    @Override
    public String explainException(String type, String message, String stackTrace) {
        return "An exception occurred.";  // Safe generic response
    }

    @Override
    public String classifyData(String value, String context) {
        return "unknown";  // Safe classification
    }

    @Override
    public boolean isHealthy() {
        return true;  // Always healthy
    }

    @Override
    public String getServiceName() {
        return "NoOp";
    }
}
```

### OpenAIService (Real Implementation)

```java
public class OpenAIService implements AIService {
    private final String apiKey;
    private final String model;
    private final LRUCache<String, String> cache;

    @Override
    public String sanitize(String value, String context) throws AIServiceException {
        try {
            // Call OpenAI API with prompt
            String prompt = "Sanitize this value: " + value;
            return callApi(prompt);
        } catch (TimeoutException e) {
            throw new AIServiceException("Timeout calling OpenAI API", e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // Quick health check
            callApi("test");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ... other methods
}
```

---

## 📝 Exception Handling

### AIServiceException

```java
public class AIServiceException extends Exception {
    public AIServiceException(String message) {
        super(message);
    }

    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**When thrown:**
- API authentication failure (invalid key)
- Network timeout
- Malformed response
- Rate limit exceeded
- Invalid configuration

**How handled:**
- Caught by LogGuardLayout
- Falls back to rule-based masking
- Logs warning, continues
- Does NOT break logging

**Example:**
```java
try {
    String sanitized = service.sanitize(value, context);
} catch (AIServiceException e) {
    logger.warn("AI service failed, using rule-based masking", e);
    // Fall back to SanitizationEngine
}
```

---

## 🔄 Usage Patterns

### Pattern 1: Direct Usage

```java
AIConfig config = new AIConfig();
config.setApiKey(System.getenv("OPENAI_API_KEY"));

AIService service = AIServiceFactory.createService(config);

String sanitized = service.sanitize("sk-secret", "apiKey");
```

### Pattern 2: With Caching

```java
AIService baseService = AIServiceFactory.createService(config);
CachedAIService cachedService = 
    new CachedAIService(baseService, 1000, 3600000L);

String sanitized = cachedService.sanitize("sk-secret", "apiKey");
// Cached on second call!
```

### Pattern 3: With Health Check

```java
AIService service = AIServiceFactory.createService(config);

if (service.isHealthy()) {
    // Good to use
    String result = service.sanitize(...);
} else {
    // Use fallback
    logger.warn("AI service unavailable");
}
```

---

## 🎯 When to Call Each Method

| Method | When | Example |
|--------|------|---------|
| `sanitize()` | Processing high-risk values | apiKey, password, token |
| `explainException()` | Logging exceptions | Caught in try/catch |
| `classifyData()` | Deciding masking strategy | Determine if PII |
| `isHealthy()` | Startup, monitoring | Health checks |
| `getServiceName()` | Logging, debugging | Show which service |

---

## 🔗 Related Documentation

- **Configuration:** [All config options](../guides/configuration.md)
- **Components:** [Implementation details](../architecture/components.md)
- **Quick Start:** [Get started](../versions/v0.2/QUICK_START.md)

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)

---

*Last Updated: 2026-04-23*

[← Back to API Reference](README.md)
