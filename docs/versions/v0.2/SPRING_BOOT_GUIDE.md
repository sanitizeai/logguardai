# Using LogGuardAI in Spring Boot

Complete guide for integrating LogGuardAI with Spring Boot applications.

**Compatibility:** Spring Boot 2.x and 3.x | Java 8+  
**LogGuardAI Version:** 0.3.0+ (recommended) | 0.2.0+ (supported)

---

## 🎯 Quick Answer

**Log4j2: ✅ YES** (fully supported)  
**Logback: ⚠️ NO** (not supported, different architecture)

LogGuardAI is a **Log4j2 plugin**. Spring Boot defaults to Logback, so you need to **explicitly use Log4j2** instead.

---

## 📋 Option 1: Spring Boot with Log4j2 (Recommended)

**v0.3.0+ ✨ NEW:** Proper Log4j2 plugin registration - no package scanning needed!

### Step 1: Update pom.xml

Remove default Logback, add Log4j2:

```xml
<dependencies>
    <!-- Spring Boot Starter (without default Logback) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-logging</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Add Log4j2 instead -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-log4j2</artifactId>
    </dependency>

    <!-- Add LogGuardAI (Log4j2 plugin) -->
    <!-- v0.3.0+: Proper plugin registration, no packages attribute needed -->
    <dependency>
        <groupId>com.logguardai</groupId>
        <artifactId>logguardai</artifactId>
        <version>0.3.0</version>
    </dependency>

    <!-- Other Spring Boot starters as needed -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### Step 2: Create log4j2.xml

Create `src/main/resources/log4j2.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
  <Properties>
    <!-- Define properties for easy configuration -->
    <Property name="log.level">info</Property>
    <Property name="log.dir">logs</Property>
  </Properties>

  <Appenders>
    <!-- Console with LogGuardAI (development) -->
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout
          aiEnabled="false"/>
      <PatternLayout pattern="%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"/>
    </Console>

    <!-- File with LogGuardAI (production) -->
    <RollingFile 
        name="File" 
        fileName="${log.dir}/app.log"
        filePattern="${log.dir}/app-%d{yyyy-MM-dd}-%i.log">
      <LogGuardLayout
          aiEnabled="true"
          aiProvider="openai"
          aiApiKey="${OPENAI_API_KEY}"
          aiModel="gpt-3.5-turbo"
          aiThreshold="5"
          aiTimeoutMs="2000"
          samplingRate="0.1"/>
      <PatternLayout pattern="%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"/>
      <Policies>
        <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
        <SizeBasedTriggeringPolicy size="10MB"/>
      </Policies>
    </RollingFile>
  </Appenders>

  <Loggers>
    <!-- Your application loggers -->
    <Logger name="com.yourcompany" level="debug"/>
    <Logger name="org.springframework" level="info"/>
    
    <!-- Root logger -->
    <Root level="${log.level}">
      <AppenderRef ref="Console"/>
      <AppenderRef ref="File"/>
    </Root>
  </Loggers>
</Configuration>
```

### Step 3: Set Environment Variable

```bash
export OPENAI_API_KEY="sk-..."
```

Or in `application.properties`:

```properties
# application.properties
# Note: Log4j2 reads ${OPENAI_API_KEY} from environment, 
# not Spring properties. Set it as environment variable first!
```

### Step 4: Run Spring Boot Application

```bash
java -jar myapp.jar
```

Or with Maven:

```bash
mvn spring-boot:run
```

---

## 🎨 Configuration Profiles (Spring Boot)

### application.properties - Development

```properties
# src/main/resources/application-dev.properties
spring.application.name=my-app
logging.config=classpath:log4j2-dev.xml
```

### log4j2-dev.xml - Development (Low Overhead)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout aiEnabled="false"/>
      <PatternLayout pattern="%d{HH:mm:ss} %-5level %logger{36} - %msg%n"/>
    </Console>
  </Appenders>

  <Root level="debug">
    <AppenderRef ref="Console"/>
  </Root>
</Configuration>
```

### application.properties - Production

```properties
# src/main/resources/application-prod.properties
spring.application.name=my-app
logging.config=classpath:log4j2-prod.xml
```

### log4j2-prod.xml - Production (With AI)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
  <Appenders>
    <!-- Console (minimal) -->
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout aiEnabled="false"/>
      <PatternLayout pattern="%d{ISO8601} %-5level %msg%n"/>
    </Console>

    <!-- File with AI sanitization -->
    <RollingFile 
        name="File" 
        fileName="logs/app.log"
        filePattern="logs/app-%d{yyyy-MM-dd}-%i.log">
      <LogGuardLayout
          aiEnabled="true"
          aiProvider="openai"
          aiApiKey="${OPENAI_API_KEY}"
          aiModel="gpt-3.5-turbo"
          aiThreshold="5"
          aiTimeoutMs="2000"
          samplingRate="0.1"/>
      <PatternLayout pattern="%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n"/>
      <Policies>
        <TimeBasedTriggeringPolicy interval="1" modulate="true"/>
        <SizeBasedTriggeringPolicy size="50MB"/>
      </Policies>
      <DefaultRolloverStrategy max="30"/>
    </RollingFile>
  </Appenders>

  <Root level="info">
    <AppenderRef ref="Console"/>
    <AppenderRef ref="File"/>
  </Root>
</Configuration>
```

### Run with Profile

```bash
# Development (no AI)
java -jar myapp.jar --spring.profiles.active=dev

# Production (with AI)
java -jar myapp.jar --spring.profiles.active=prod \
  -DOPENAI_API_KEY="sk-..."
```

---

## 💡 Spring Boot Usage Examples

### Example 1: Basic Controller Logging

```java
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // This will be automatically sanitized by LogGuardAI
        logger.info("Login attempt: username={} apiKey={} token={}",
            request.getUsername(), 
            request.getApiKey(),      // Will be masked
            request.getToken());       // Will be masked

        // Process login...
        return ResponseEntity.ok("Success");
    }
}
```

### Example 2: Service Logging with Secrets

```java
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    public void processPayment(PaymentRequest payment) {
        // Secrets automatically sanitized
        logger.info("Processing payment: userId={} cardNumber={} cvv={} amount={}",
            payment.getUserId(),
            payment.getCardNumber(),   // Will be masked
            payment.getCvv(),          // Will be masked
            payment.getAmount());

        // Process payment...
    }
}
```

### Example 3: Exception Logging

```java
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public User getUser(String userId) {
        try {
            // Some operation...
            return findUser(userId);
        } catch (RuntimeException e) {
            // With v0.2 AI: Enhanced exception explanations
            logger.error("Failed to retrieve user: {}", userId, e);
            // Log: Enhanced insight about what went wrong + suggestions
            throw new UserNotFoundException("User not found", e);
        }
    }
}
```

---

## 📊 Spring Boot with LogGuardAI - Full Example

### Project Structure

```
my-spring-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── Application.java
│   │   │   ├── controller/
│   │   │   │   └── ApiController.java
│   │   │   └── service/
│   │   │       └── DataService.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── log4j2.xml
│   │       ├── log4j2-dev.xml
│   │       └── log4j2-prod.xml
│   └── test/
│       └── java/...
└── logs/
    └── app.log
```

### pom.xml (Complete)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-spring-app</artifactId>
    <version>1.0.0</version>
    <name>My Spring Boot App with LogGuardAI</name>

    <properties>
        <java.version>11</java.version>
    </properties>

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Spring Boot Web Starter (no Logback) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Log4j2 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>

        <!-- LogGuardAI -->
        <dependency>
            <groupId>com.github.sanitizeai</groupId>
            <artifactId>logguardai</artifactId>
            <version>v0.3.0</version>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Application.java

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### ApiController.java

```java
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @PostMapping("/data")
    public ResponseEntity<?> submitData(@RequestBody DataRequest request) {
        // Automatically sanitized by LogGuardAI
        logger.info("Data received: userId={} apiKey={} data={}",
            request.getUserId(),
            request.getApiKey(),      // MASKED
            request.getData());

        return ResponseEntity.ok("Received");
    }
}
```

---

## ⚠️ Logback: Why Not Supported

### Why LogGuardAI doesn't work with Logback

Logback and Log4j2 have **different architectures**:

| Aspect | Log4j2 | Logback |
|--------|--------|---------|
| **Plugin System** | ✅ Plugin API | ❌ No plugin system |
| **Custom Layouts** | ✅ Supported | ⚠️ Limited support |
| **Extension Point** | ✅ Layout plugins | ❌ No standardized way |
| **LogGuardAI** | ✅ Built as plugin | ❌ Not compatible |

### Logback Concerns

Logback uses a different configuration model and doesn't have the same plugin architecture as Log4j2. While theoretically you could:
- Wrap Logger calls
- Use custom filters
- Implement a converter

These approaches would be:
- **Complex** - lots of boilerplate
- **Inefficient** - not optimized
- **Fragile** - tight coupling to Logback internals
- **Not recommended**

---

## ✅ Best Practices for Spring Boot + LogGuardAI

### 1. Use Environment Variables for Secrets

```bash
# DO NOT hardcode API keys!
export OPENAI_API_KEY="sk-..."
java -jar myapp.jar
```

### 2. Use Different Configs Per Environment

```bash
# Development
java -jar myapp.jar --spring.profiles.active=dev

# Production
java -jar myapp.jar --spring.profiles.active=prod
```

### 3. Keep AI Optional

```xml
<!-- Dev: Disable AI for faster startup -->
<LogGuardLayout aiEnabled="false"/>

<!-- Prod: Enable AI for better sanitization -->
<LogGuardLayout aiEnabled="true" samplingRate="0.1"/>
```

### 4. Monitor Cache Performance

```java
@Component
public class CacheMonitor {
    private final Logger logger = LoggerFactory.getLogger(CacheMonitor.class);

    @Scheduled(fixedRate = 60000) // Every minute
    public void reportCacheStats() {
        // Monitor cache hit rate
        logger.info("Cache stats: {}"); // Will show cache performance
    }
}
```

### 5. Handle Slow Logs

```xml
<!-- Increase timeout for slower networks -->
<LogGuardLayout
    aiTimeoutMs="3000"
    samplingRate="0.05"/>
```

---

## 🚀 Deployment

### Docker Setup

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy JAR
COPY target/my-spring-app-1.0.0.jar app.jar

# Set environment variable
ENV OPENAI_API_KEY=${OPENAI_API_KEY}

# Create logs directory
RUN mkdir -p /app/logs

# Run
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

### Kubernetes ConfigMap (Optional)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: logguardai-config
data:
  log4j2.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <Configuration packages="com.logguardai">
      <!-- Your configuration -->
    </Configuration>
```

---

## 📞 Troubleshooting

### "LogGuardLayout not found" Error (v0.2.0 and earlier)

**This is fixed in v0.3.0+**

**For v0.3.0+:** LogGuardAI now uses proper Log4j2 plugin registration. No `packages` attribute needed.

```xml
<!-- v0.3.0+: Works without packages attribute -->
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="warn">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <LogGuardLayout aiEnabled="false"/>
      <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
  </Appenders>
</Configuration>
```

**For v0.2.0:** If you're still on v0.2.0, the workaround is to include `packages="com.logguardai"`:

```xml
<!-- v0.2.0 workaround (upgrade to 0.3.0+ recommended) -->
<?xml version="1.0" encoding="UTF-8"?>
<Configuration packages="com.logguardai" status="warn">
  <!-- Configuration continues -->
</Configuration>
```

### "Logback is being used instead of Log4j2"

**Solution:** Ensure you excluded spring-boot-starter-logging:

```xml
<exclusion>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</exclusion>
```

### "LogGuardLayout not found"

**Solution:** Verify dependency and package declaration:

```xml
<Configuration packages="com.logguardai">
    <!-- Ensure this line is present -->
</Configuration>
```

### "API calls not happening"

**Solution:** Check configuration:
```xml
<LogGuardLayout
    aiEnabled="true"  <!-- Must be true -->
    aiApiKey="${OPENAI_API_KEY}"  <!-- Must be set -->
    samplingRate="0.5"/>  <!-- Increase for testing -->
```

---

## 🎓 Complete Spring Boot Example

See full working example at:  
→ [Spring Boot + LogGuardAI Sample Project](../guides/project-structure.md)

---

## 📖 Related Documentation

- **[Configuration Reference](../guides/configuration.md)** — All options
- **[Troubleshooting](../guides/troubleshooting.md)** — Common issues
- **[AI Integration](./AI_GUIDE.md)** — AI features
- **[Quick Start](./QUICK_START.md)** — Basic setup

---

## ✨ Next Steps

1. **Add Log4j2 to pom.xml** (exclude Logback)
2. **Create log4j2.xml** configuration
3. **Set OPENAI_API_KEY** environment variable
4. **Start application** - logs automatically sanitized!

---

*Spring Boot + LogGuardAI = Secure logs, automatically.*

[← Back to v0.2 Docs](README.md)
