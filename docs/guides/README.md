# Guides & References

General guides and references that apply across all versions.

---

## 📚 Available Guides

### Core Guides

| Guide | Purpose | Audience |
|-------|---------|----------|
| **[Configuration Reference](configuration.md)** | All config options explained | DevOps, Developers |
| **[Troubleshooting](troubleshooting.md)** | Common issues & solutions | Everyone |
| **[JitPack Publishing](jitpack-publishing.md)** | Publish LogGuardAI to JitPack | Contributors |
| **[Project Structure](project-structure.md)** | Codebase organization | Contributors |

---

## 🎯 Quick Links

### I need to...

- **Configure LogGuardAI** → [Configuration Reference](configuration.md)
- **Fix an error** → [Troubleshooting](troubleshooting.md)
- **Understand the code** → [Project Structure](project-structure.md)
- **Publish a release** → [JitPack Publishing](jitpack-publishing.md)
- **Get started quickly** → [Quick Start](../versions/v0.2/QUICK_START.md)
- **Learn about features** → [AI Integration Guide](../versions/v0.2/AI_GUIDE.md)

---

## 📖 By Version

- **v0.2** (Current) → [v0.2 Docs](../versions/v0.2/)
- **v0.1** (Previous) → [v0.1 Docs](../versions/v0.1/)

---

## 💡 Configuration Examples

### Simple (Rule-Based Only)
```xml
<LogGuardLayout/>
```

### With AI (10% sampling)
```xml
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    samplingRate="0.1"/>
```

### Production (High Security)
```xml
<LogGuardLayout
    aiEnabled="true"
    aiProvider="openai"
    aiApiKey="${OPENAI_API_KEY}"
    aiModel="gpt-4"
    samplingRate="1.0"/>
```

See [Configuration Reference](configuration.md) for all options.

---

## 🚀 Quick Troubleshooting

| Problem | Solution |
|---------|----------|
| App won't start | [See troubleshooting guide](troubleshooting.md#-application-wont-start) |
| No API calls | Check `aiEnabled="true"` and API key |
| Logs are slow | Lower `samplingRate` or reduce timeout |
| High memory | Check cache size and log volume |
| Not sanitizing | Verify LogGuardLayout is on appender |

Full guide: [Troubleshooting](troubleshooting.md)

---

## 📞 Support

- **Questions?** [GitHub Discussions](https://github.com/sanitizeai/logguardai/discussions)
- **Issues?** [GitHub Issues](https://github.com/sanitizeai/logguardai/issues)
- **Docs?** [Documentation Index](../README.md)

---

*Last Updated: 2026-04-23*

[← Back to Documentation](../README.md)
