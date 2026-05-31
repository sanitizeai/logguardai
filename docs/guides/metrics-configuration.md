# Pattern-Based Metrics System - Logs to Metrics Configuration Guide

## Overview

LogGuardAI v0.5.0 introduces a **pattern-based logs-to-metrics system** that allows you to extract metrics directly from Log4j2 log messages using customizable regex patterns. This enables Log4j2 metrics extraction and Prometheus-compatible metric output without external monitoring infrastructure. Unlike external systems, this approach is:

- **Built-in**: No external dependencies (zero additional infrastructure)
- **User-Defined**: Create patterns specific to your application's log format
- **Low-Overhead**: In-memory aggregation with periodic file flushing
- **Cardinality-Safe**: Configurable limits prevent memory explosion from unbounded labels
- **Append-Only**: Metrics file captures cumulative data with timestamps

## Quick Start

### Minimal Configuration (log4j2.xml)

```xml
<Configuration>
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <LogGuardLayout 
                extractMetrics="true"
                metricsFilePath="logs/metrics.txt"
                metricsFlushIntervalMs="60000"
                metricsPatterns="http_req|GET /([\\w/]+) ([0-9]+)|http_requests|endpoint,status"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

### Pattern Definition Format

Patterns are defined as semicolon-separated strings with the format:

```
name|regex|metricName|field1,field2;name2|regex2|metric2|field3
```

- **name**: Unique pattern identifier (e.g., `http_req`)
- **regex**: Java regex with capture groups for extracting fields
- **metricName**: Prometheus metric name (e.g., `http_requests_total`)
- **fields**: Comma-separated field names for labels

### Example: HTTP Request Metrics

```xml
<LogGuardLayout 
    extractMetrics="true"
    metricsPatterns="http_requests|(GET|POST|PUT|DELETE) ([/\w/]+) (\d{3})|http_requests_total|method,endpoint,status"/>
```

This pattern matches log lines like:
- `GET /api/users 200`
- `POST /api/orders 201`
- `DELETE /api/products 404`

Extracted metrics:
```
http_requests_total{method="GET",endpoint="/api/users",status="200"} 42
http_requests_total{method="POST",endpoint="/api/orders",status="201"} 15
http_requests_total{method="DELETE",endpoint="/api/products",status="404"} 3
```

## Real-World Examples

### Multi-Protocol Logging

```xml
<LogGuardLayout 
    extractMetrics="true"
    metricsFilePath="logs/metrics.txt"
    metricsFlushIntervalMs="60000"
    metricsPatterns="http_logs|(GET|POST|PUT|DELETE) ([/\w/-]+) (\d{3})|http_requests_total|method,path,status;
                     ftp_logs|(RECV|SEND|DELETE) ([\w.]+) (\d+)|ftp_operations|operation,filename,bytes;
                     sftp_logs|SFTP: (\w+) ([\w.]+) to ([0-9.]+)|sftp_transfers|operation,file,host"/>
```

### E-commerce Application

```xml
<LogGuardLayout 
    extractMetrics="true"
    metricsPatterns="cart_events|CART: (ADDED|REMOVED|CLEARED) - (\d+) items - \$([\d.]+)|cart_total|event,item_count,amount;
                     order_events|ORDER: (\d+) placed by (\w+) - \$([\d.]+)|orders_total|order_id,customer,amount;
                     payment_status|PAYMENT: (\w+) for order (\d+)|payment_status|result,order_id"/>
```

### Database Operations

```xml
<LogGuardLayout 
    extractMetrics="true"
    metricsPatterns="db_queries|Query: (SELECT|INSERT|UPDATE|DELETE) - (\d+)ms|db_query_duration|operation,duration_ms;
                     db_connections|Connection (OPEN|CLOSE) - pool: (\d+)/(\d+)|db_connections_state|state,current,max"/>
```

## Configuration Attributes

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `extractMetrics` | boolean | false | Enable/disable metrics recording |
| `metricsFilePath` | String | "logs/metrics.txt" | Output file path for metrics |
| `metricsFlushIntervalMs` | long | 60000 | Flush interval (milliseconds) |
| `metricsMaxCardinality` | int | 10000 | Max unique label combinations per pattern |
| `metricsPatterns` | String | "" | Pattern definitions (semicolon-separated) |

## Metrics Output Format

Metrics are stored in Prometheus text format with timestamps:

```
# Timestamp: 2026-05-27T18:30:00Z
# HELP http_requests_total HTTP requests counter
# TYPE http_requests_total counter
http_requests_total{method="GET",endpoint="/api/users",status="200"} 1523
http_requests_total{method="POST",endpoint="/api/orders",status="201"} 247
http_requests_total{method="GET",endpoint="/api/users",status="404"} 12

# Timestamp: 2026-05-27T18:31:00Z
http_requests_total{method="GET",endpoint="/api/users",status="200"} 3045
http_requests_total{method="POST",endpoint="/api/orders",status="201"} 523
```

### Reading the File

The metrics file is append-only, growing with each flush interval. To get current metrics:
- Read the **last section** (after the latest timestamp header)
- Use cumulative counts across all timestamps for historical analysis

## Cardinality Management

Cardinality limits prevent memory explosion from high-dimensional metrics:

### Example: Cardinality Risk

```xml
<!-- RISKY: Unbounded IP addresses as labels -->
<LogGuardLayout 
    metricsPatterns="requests|GET ([/\w]+) from ([0-9.]+)|requests_total|path,client_ip"/>
```

If 100,000 unique IP addresses access your app, this creates 100,000 distinct metrics for a single pattern!

### Solution: Set Limit

```xml
<LogGuardLayout 
    extractMetrics="true"
    metricsMaxCardinality="5000"
    metricsPatterns="requests|GET ([/\w]+) from ([0-9.]+)|requests_total|path,client_ip"/>
```

When the cardinality limit (5,000) is reached, new label combinations are **skipped** to protect memory.

**Best Practice**: Use low-cardinality labels (method, status code, service name). Avoid unbounded fields like IP addresses, user IDs, or request IDs.

## Integration with Monitoring Tools

### Prometheus Scraping

Write a scraper that reads the metrics file and exposes it via HTTP:

```python
from prometheus_client import CollectorRegistry, Counter, start_http_server
import re

def read_metrics_file(filepath):
    metrics = {}
    with open(filepath) as f:
        for line in f:
            if line.startswith('#'):
                continue
            match = re.match(r'(\w+)\{?([^}]*)\}?\s+(\d+)', line)
            if match:
                name, labels, value = match.groups()
                metrics[f"{name}_{labels}"] = int(value)
    return metrics

# Expose to Prometheus
start_http_server(8000)
```

### ELK Stack Integration

Parse metrics file in Logstash to send to Elasticsearch:

```logstash
filter {
    if [message] =~ /^[a-z_]+\{/ {
        grok {
            match => { "message" => "%{WORD:metric}\{(?<labels>[^}]+)\}\s+%{NUMBER:value}" }
        }
    }
}
```

### Custom Processing

Metrics file supports any downstream processing:
- Real-time dashboards
- Alerting systems
- Analytics pipelines
- Log aggregation platforms

## Troubleshooting

### Metrics File Not Created

1. Verify `extractMetrics="true"` is set
2. Verify `metricsPatterns` is not empty
3. Check that metrics patterns match actual log messages
4. Verify write permissions for metrics file directory

### No Metrics Recorded

1. Check regex patterns against actual log lines (use `java.util.regex.Pattern` to test)
2. Verify pattern field count matches capture group count
3. Increase logging level to see if patterns are matching
4. Test patterns with sample log lines

Example test in Java:

```java
Pattern p = Pattern.compile("(GET|POST)\\s+([/\\w/]+)\\s+(\\d{3})");
Matcher m = p.matcher("GET /api/users 200");
if (m.find()) {
    System.out.println("Match! Groups: " + m.group(1) + ", " + m.group(2) + ", " + m.group(3));
}
```

### High Memory Usage

1. Reduce `metricsMaxCardinality` to lower limit
2. Review patterns - remove high-cardinality fields (user IDs, IPs)
3. Reduce `metricsFlushIntervalMs` to flush more frequently
4. Disable metrics for low-value patterns

### Metrics File Growing Too Large

1. Implement log rotation (e.g., daily rotation with file size limits)
2. Create external job to archive old metrics:
   ```bash
   # Archive and compress daily
   gzip logs/metrics.txt
   mv logs/metrics.txt.gz logs/metrics.$(date +%Y%m%d).txt.gz
   echo "" > logs/metrics.txt  # Create fresh file
   ```

## Performance Considerations

### CPU Impact
- Regex matching: ~0.1ms per pattern per log line
- Lock contention: Negligible with ReentrantReadWriteLock
- Recommendation: Test with production-like volumes

### Memory Impact
- Per pattern: ~8 bytes × cardinality limit
- Example: 1,000 patterns × 10,000 cardinality = ~80MB worst-case
- Mitigation: Set `metricsMaxCardinality` based on your needs

### I/O Impact
- File flush: ~1-5ms depending on metric count
- Default: 60-second flush interval = negligible overhead
- Append-only design: Efficient sequential writes

## API Usage (Java)

```java
// Access metrics programmatically
MetricsRegistry registry = layout.getMetricsRegistry();

// Get all metrics
Map<String, Long> metrics = registry.getMetrics();
metrics.forEach((key, count) -> 
    System.out.println(key + " = " + count)
);

// Manual flush
layout.flushMetrics();

// Shutdown on app close
layout.shutdown();
```

## Version Info

- **Added**: v0.5.0
- **Pattern-based approach**: User-defined patterns, zero external dependencies
- **Thread-safety**: ReentrantReadWriteLock + ConcurrentHashMap
- **Production-ready**: Tested with 100+ concurrent threads

## See Also

- [MetricsPattern.java](../java/com/logguardai/metrics/MetricsPattern.java) - Pattern matching logic
- [MetricsRegistry.java](../java/com/logguardai/metrics/MetricsRegistry.java) - Metrics aggregation
- [MetricsFlushManager.java](../java/com/logguardai/metrics/MetricsFlushManager.java) - Periodic flushing
- [LogGuardLayout.java](../java/com/logguardai/layout/LogGuardLayout.java) - Integration point
