# Architecture Index

System design and architecture documentation for LogGuardAI.

---

## 📚 Architecture Docs

| Document | Purpose |
|----------|---------|
| **[System Overview](overview.md)** | How LogGuardAI works (processing pipeline) |
| **[Details & Components](components.md)** | Component descriptions & interactions |

---

## 🎯 Quick Links

- **"How does LogGuardAI work?"** → [System Overview](overview.md)
- **"What are the components?"** → [Components](components.md)
- **"How is it designed?"** → [Design Principles](overview.md#-design-principles)
- **"What about performance?"** → [Performance](overview.md#-performance-characteristics)
- **"How does AI integration work?"** → [AI Layer](overview.md#-v02-enhancements)

---

## 📊 Architecture at a Glance

```
Log Event
    ↓
[Tokenize] Extract key=value pairs
    ↓
[Score] Calculate risk (0-30+)
    ↓
[Decide] Pass → Mask → AI
    ↓
[Sanitize] Apply transformation
    ↓
Sanitized Log (no secrets!)
```

---

## 🔑 Key Concepts

### Processing Pipeline
LogGuardAI intercepts logs and routes them through a decision pipeline:
1. Parse & tokenize
2. Assess risk
3. Decide action
4. Apply sanitization
5. Output result

### Risk Scoring
Multi-factor analysis determines how "secret" a value is:
- Keyword analysis (is "password" in key?)
- Pattern detection (looks like JWT?)
- Entropy analysis (random-looking?)
- Context analysis (near other secrets?)

### Intelligent Safeguards
- **Cache:** 80-95% hit rate, <1ms response
- **Timeout:** 2 seconds max wait
- **Fallback:** Always revert to rule-based
- **Non-blocking:** Never slows application

---

## 📈 v0.1 vs v0.2

### v0.1: Rule-Based (Fast)
```
Score → Decide → Mask
Simple, reliable, no external API
```

### v0.2: AI-Enhanced (Smart)
```
Score → Decide → Cache/Timeout/AI → Mask
More accurate, optional, cached
```

Both available in v0.2 (AI is optional!)

---

## 🔗 Related Documents

- **Configuration:** [All config options](../guides/configuration.md)
- **Quick Start:** [5-minute setup](../versions/v0.2/QUICK_START.md)
- **AI Guide:** [AI features](../versions/v0.2/AI_GUIDE.md)
- **Project Structure:** [Code navigation](../guides/project-structure.md)

---

## 💡 Design Highlights

✅ **Non-Blocking** — Logs finish instantly  
✅ **Fail-Safe** — Logging never breaks  
✅ **Modular** — Easy to extend  
✅ **Observable** — Built-in metrics  
✅ **Secure** — Secrets never exposed  
✅ **Fast** — <5ms rule-based, <1ms cached  

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)

---

*Last Updated: 2026-04-23*

[← Back to Documentation](../README.md)
