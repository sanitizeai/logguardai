# Package
> src/main/java/com/logguardai/
## Core Components
- `model/` - Token and configuration models
- `tokenizer/` - LogTokenizer for parsing
- `scoring/` - RiskScoringEngine and DecisionEngine
- `sanitizer/` - SanitizationEngine for masking
- `exception/` - ExceptionProcessor for insights
- `layout/` - LogGuardLayout (Log4j2 plugin)

# Tests
> src/test/java/com/logguardai/
- `LogTokenizerTest` - Tokenization tests
- `RiskScoringTest` - Risk scoring tests
- `DecisionEngineTest` - Decision logic tests
- `SanitizationEngineTest` - Masking tests
- `ExceptionProcessorTest` - Exception handling tests

# Configuration
> src/main/resources/
- `log4j2.xml` - Configuration example

# Build
- `pom.xml` - Maven configuration

# Documentation
- `README.md` - Usage guide
- `architecture-instructions.md` - Design specification

---

## Quick Navigation

**To build:** `mvn clean package`

**To test:** `mvn test`

**To run example:** `mvn compile exec:java -Dexec.mainClass="com.logguardai.example.ExampleApp"`

**To integrate:** Add dependency to your pom.xml and configure log4j2.xml
