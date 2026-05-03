# JitPack Publishing Guide for LogGuardAI

## ✅ What's Done

- ✅ Git repository initialized
- ✅ All source files committed
- ✅ Remote configured: `https://github.com/sanitizeai/logguardai.git`
- ✅ Ready to publish

---

## 🚀 Step 1: Push to GitHub

Push the committed code to your GitHub repository:

```bash
cd /opt/development/logguardai
git push -u origin main
```

**Note:** You may need to use:
- **SSH key** (if configured): `git push -u origin main`
- **GitHub CLI**: `gh auth login` then `git push -u origin main`
- **Personal Access Token**: When prompted for password, use your GitHub PAT instead

---

## 📦 Step 2: Create a GitHub Release

JitPack automatically builds from GitHub **releases**. Create one for v0.1.0:

### Option A: Via GitHub Web UI
1. Go to: https://github.com/sanitizeai/logguardai/releases
2. Click "Create a new release"
3. **Tag version:** `v0.1.0`
4. **Release title:** `LogGuardAI v0.1.0 - Initial Release`
5. **Description:**
   ```
   Initial release of LogGuardAI - Log4j2 extension for context-aware log sanitization
   
   ## Features
   - Context-aware tokenization (key=value, JSON)
   - Risk scoring engine with heuristics
   - Rule-based sensitive data masking
   - Exception insights for 10+ common exceptions
   - Non-blocking, fail-safe design
   - Log4j2 integration
   
   ## What's Included
   - Core sanitization pipeline
   - 29 unit tests (all passing)
   - Example configuration & usage
   - Comprehensive documentation
   ```
6. Click "Publish release"

### Option B: Via Git Command
```bash
cd /opt/development/logguardai
git tag -a v0.1.0 -m "LogGuardAI v0.1.0 - Initial Release"
git push origin v0.1.0
```

---

## ✨ Step 3: JitPack Auto-Build

Once the release is created, JitPack will automatically start building:

1. Visit: https://jitpack.io/#sanitizeai/logguardai
2. You should see `v0.1.0` in the list
3. Click the `Log` button to see build status
4. Once it shows a green checkmark, it's ready to use!

---

## 📥 Step 4: Use LogGuardAI via JitPack

### Add JitPack Repository

In your `pom.xml`, add the repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Add LogGuardAI Dependency

```xml
<dependency>
    <groupId>com.github.sanitizeai</groupId>
    <artifactId>logguardai</artifactId>
    <version>v0.1.0</version>
</dependency>
```

**Note:** The `groupId` is `com.github.` + your GitHub username (`sanitizeai`)

---

## 🧪 Complete pom.xml Example

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- LogGuardAI from JitPack -->
        <dependency>
            <groupId>com.github.sanitizeai</groupId>
            <artifactId>logguardai</artifactId>
            <version>v0.1.0</version>
        </dependency>
        
        <!-- Other dependencies -->
    </dependencies>
</project>
```

---

## 📋 Configure in Your Project

1. Add the JitPack repository
2. Add the dependency as shown above
3. Add LogGuardAI to your `log4j2.xml`:

```xml
<Configuration>
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

4. Start using it:

```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyApp {
    private static final Logger logger = LogManager.getLogger(MyApp.class);

    public static void main(String[] args) {
        // Sensitive data automatically masked
        logger.info("User login: userId=123456789 token=abc123xyz");
        // Output: User login: userId=***** token=*****
    }
}
```

---

## 🔗 JitPack Links

- **JitPack Project:** https://jitpack.io/#sanitizeai/logguardai
- **GitHub Repository:** https://github.com/sanitizeai/logguardai
- **Release Page:** https://github.com/sanitizeai/logguardai/releases

---

## 📊 Version Management

For future releases, just:

1. Update version in `pom.xml` (if desired)
2. Create a commit
3. Create a new GitHub release with the tag
4. JitPack automatically builds it

**Example:** For v0.4.0:
```bash
git tag -a v0.4.0 -m "LogGuardAI v0.4.0 - Multi-Provider AI Support"
git push origin v0.4.0
```

Then use in pom.xml:
```xml
<version>v0.4.0</version>
```

---

## ✅ Checklist

- [ ] Push code to GitHub: `git push -u origin main`
- [ ] Create GitHub release: `v0.1.0`
- [ ] Verify JitPack build: https://jitpack.io/#sanitizeai/logguardai
- [ ] Green checkmark appears for v0.1.0
- [ ] Add JitPack repository to your consumer pom.xml
- [ ] Add LogGuardAI dependency
- [ ] Update log4j2.xml configuration
- [ ] Run `mvn clean install`
- [ ] Test with example code

---

## 🆘 Troubleshooting

### "No builds found"
- Make sure the GitHub release tag is pushed: `git push origin v0.1.0`
- Wait a few minutes for JitPack to detect it

### "Build failed"
- Check JitPack build log at: https://jitpack.io/#sanitizeai/logguardai/v0.1.0
- Common issues: Wrong pom.xml, missing dependencies, or version mismatch

### "Dependency not found"
- Verify the repository URL is correct
- Check groupId: `com.github.sanitizeai` (GitHub username must be lowercase)
- Confirm version tag exists on GitHub

### Build locally to test first
```bash
mvn clean package
mvn test
```

---

## 🎓 About JitPack

**JitPack** is a package repository that builds from GitHub automatically:
- No need to publish to Maven Central Repository
- Supports any GitHub repository with Maven/Gradle
- Automatic build from any Git tag/release
- Free hosting
- Perfect for libraries and tools

**Resources:**
- https://jitpack.io
- https://docs.jitpack.io

---

**Status:** Ready for publishing! 🚀

Follow steps 1-3 above to make LogGuardAI available via JitPack.
