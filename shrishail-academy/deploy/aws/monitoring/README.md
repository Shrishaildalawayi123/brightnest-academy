# 📊 BrightNest Academy - Production Monitoring Setup

Complete observability stack using **Prometheus + Grafana** with exporters for application, infrastructure, database, and container metrics.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Monitoring Stack                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐      ┌──────────────┐                    │
│  │   Grafana    │◄─────┤  Prometheus  │                    │
│  │  Port 3000   │      │  Port 9090   │                    │
│  └──────────────┘      └──────┬───────┘                    │
│                               │                             │
│                    ┌──────────┼──────────────┐              │
│                    │          │              │              │
│          ┌─────────▼───┐  ┌───▼────────┐  ┌─▼──────────┐   │
│          │ Spring Boot │  │   MySQL    │  │   Redis    │   │
│          │  Actuator   │  │  Exporter  │  │  Exporter  │   │
│          │ :8080/      │  │   :9104    │  │   :9121    │   │
│          │ actuator/   │  └────────────┘  └────────────┘   │
│          │ prometheus  │                                    │
│          └─────────────┘  ┌────────────┐  ┌──────────────┐ │
│                           │    Node    │  │   cAdvisor   │ │
│                           │  Exporter  │  │  (Containers)│ │
│                           │   :9100    │  │    :8088     │ │
│                           └────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### 1. Prepare Environment File

Create `.env` in `/opt/brightnest/` with database credentials:

```bash
# Database (for MySQL exporter)
DB_HOST=your-rds-endpoint.amazonaws.com
DB_PORT=3306
DB_NAME=brightnest_academy
DB_USER=brightnestadmin
DB_PASS=your-db-password

# Redis (if using)
REDIS_HOST=your-redis-endpoint.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Grafana Admin
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=SecurePassword123!
```

### 2. Start Monitoring Stack

```bash
cd /opt/brightnest/monitoring
docker compose -f docker-compose.monitoring.yml up -d
```

### 3. Access Dashboards

- **Grafana**: http://localhost:3000 (or https://monitor.brightnest-academy.com if configured)
  - Username: `admin`
  - Password: `SecurePassword123!` (from .env)

- **Prometheus**: http://localhost:9090

### 4. Verify Metrics Collection

Check Prometheus targets:
```bash
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job:.labels.job, instance:.labels.instance, health:.health}'
```

Expected output (all `health: "up"`):
```json
{"job":"brightnest-spring-boot","instance":"prod-1","health":"up"}
{"job":"prometheus","instance":"localhost:9090","health":"up"}
{"job":"mysql","instance":"mysql-exporter:9104","health":"up"}
{"job":"node","instance":"ec2-prod-1","health":"up"}
{"job":"redis","instance":"redis-prod","health":"up"}
```

---

## 📈 What's Being Monitored

### Application Metrics (Spring Boot Actuator)

| Metric Category          | Examples                                               |
| ------------------------ | ------------------------------------------------------ |
| **HTTP Requests**        | Request count, latency (p50/p95/p99), status codes    |
| **JVM Memory**           | Heap usage, GC pause time, GC count                    |
| **Thread Pool**          | Active threads, queued tasks, thread creation rate     |
| **Database Connections** | Active, idle, pending, timeout, max pool size          |
| **Cache Performance**    | Hit rate, miss rate, evictions (Redis/Caffeine)        |
| **Logback**              | Error count, warn count by logger                      |
| **Custom Business**      | Login attempts, enrollments, payments (if instrumented |

**Endpoint:** `http://brightnest-academy:8080/actuator/prometheus`

### Infrastructure Metrics (Node Exporter)

| Metric            | Description                          |
| ----------------- | ------------------------------------ |
| **CPU**           | Usage by core, load average          |
| **Memory**        | Total, available, buffer/cache       |
| **Disk**          | Usage, I/O, read/write rates         |
| **Network**       | Bytes in/out, errors, dropped        |
| **System Uptime** | Time since boot                      |
| **File Systems**  | Mount points, inodes, free space     |

### Database Metrics (MySQL Exporter)

| Metric               | Description                          |
| -------------------- | ------------------------------------ |
| **Connections**      | Active, max, aborted                 |
| **Queries**          | QPS, slow queries, full table scans  |
| **InnoDB**           | Buffer pool usage, row operations    |
| **Replication**      | Lag, status (if using Multi-AZ)      |
| **Table Locks**      | Wait time, lock contention           |
| **Performance**      | Query execution time distribution    |

### Container Metrics (cAdvisor)

| Metric             | Description                          |
| ------------------ | ------------------------------------ |
| **CPU**            | Per-container CPU usage              |
| **Memory**         | Usage, limit, cache                  |
| **Network**        | RX/TX bytes per container            |
| **Disk I/O**       | Read/write operations                |
| **Restart Count**  | Container restarts (indicates crashes)|

---

## 🔔 Alert Rules

Pre-configured alerts in `alerts/application-alerts.yml`:

| Alert                        | Condition                              | Severity |
| ---------------------------- | -------------------------------------- | -------- |
| ApplicationDown              | App unreachable for 2 minutes          | Critical |
| HighErrorRate                | 5xx errors > 5% of requests            | Warning  |
| HighResponseTime             | p95 latency > 2 seconds                | Warning  |
| HighMemoryUsage              | JVM heap > 85% for 10 minutes          | Warning  |
| DatabaseConnectionPool       | DB connections > 90% of max            | Critical |
| HighCPUUsage                 | Server CPU > 80% for 10 minutes        | Warning  |
| DiskSpaceLow                 | Free disk space < 15%                  | Warning  |
| MySQLDown                    | Database unreachable                   | Critical |
| MySQLSlowQueries             | Slow queries > 0.5/sec                 | Warning  |
| FailedLoginSpike             | Failed logins > 5/sec for 5 minutes    | Warning  |

### Configure Alertmanager (Optional)

To receive alerts via email/Slack/PagerDuty:

1. Create `alertmanager.yml`:

```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'email-admin'
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'email-admin'
    email_configs:
      - to: 'ops@brightnest-academy.com'
        from: 'alerts@brightnest-academy.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alerts@brightnest-academy.com'
        auth_password: 'your-smtp-password'
```

2. Add to `docker-compose.monitoring.yml`:

```yaml
  alertmanager:
    image: prom/alertmanager:v0.26.0
    container_name: alertmanager
    restart: unless-stopped
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
    ports:
      - "127.0.0.1:9093:9093"
    networks:
      - monitoring
```

---

## 🎨 Grafana Dashboards

### Pre-Built Dashboard IDs (Import from Grafana.com)

1. **Spring Boot 2.1 Statistics** — ID: `10280`
   - JVM memory, GC, thread pools, HTTP request metrics

2. **JVM (Micrometer)** — ID: `4701`
   - Detailed JVM internals

3. **MySQL Overview** — ID: `7362`
   - Queries, connections, InnoDB buffer pool

4. **Node Exporter Full** — ID: `1860`
   - CPU, memory, disk, network

5. **Docker Container & Host Metrics** — ID: `11600`
   - cAdvisor metrics visualization

### Import Steps:

1. Login to Grafana: http://localhost:3000
2. Click **+** → **Import**
3. Enter dashboard ID (e.g., `10280`)
4. Select **Prometheus** as data source
5. Click **Import**

---

## 🔧 Customization

### Add Custom Application Metrics

In your Spring Boot service code:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

@Service
public class PaymentService {
    
    private final Counter paymentCounter;
    private final Counter paymentFailedCounter;

    public PaymentService(MeterRegistry registry) {
        this.paymentCounter = Counter.builder("payment_total")
                .description("Total payment attempts")
                .tag("tenant", "all")
                .register(registry);
        
        this.paymentFailedCounter = Counter.builder("payment_failed_total")
                .description("Failed payment attempts")
                .tag("tenant", "all")
                .register(registry);
    }

    public void processPayment(PaymentRequest req) {
        paymentCounter.increment();
        try {
            // payment logic
        } catch (Exception e) {
            paymentFailedCounter.increment();
            throw e;
        }
    }
}
```

Metrics will automatically appear in Prometheus at:
```
payment_total{tenant="all"} 1234
payment_failed_total{tenant="all"} 5
```

---

## 🌐 Production Nginx Configuration

To expose Grafana via HTTPS (subdomain):

### Nginx Config (`/etc/nginx/sites-available/monitor.brightnest-academy.com`):

```nginx
server {
    listen 443 ssl http2;
    server_name monitor.brightnest-academy.com;

    ssl_certificate /etc/letsencrypt/live/monitor.brightnest-academy.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/monitor.brightnest-academy.com/privkey.pem;

    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;

    # IP whitelist (restrict access)
    allow 1.2.3.4;  # Your office IP
    deny all;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable and reload:
```bash
sudo ln -s /etc/nginx/sites-available/monitor.brightnest-academy.com /etc/nginx/sites-enabled/
sudo certbot certonly --nginx -d monitor.brightnest-academy.com
sudo nginx -t && sudo systemctl reload nginx
```

---

## 🔍 Troubleshooting

### Metrics not showing up

```bash
# Check if Spring Boot actuator is exposing Prometheus endpoint
curl http://localhost:8080/actuator/prometheus

# Should return metrics like:
# jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 123456

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq
```

### Exporter connection errors

```bash
# Check exporter logs
docker logs mysql-exporter
docker logs node-exporter

# Test MySQL exporter connection
docker exec -it mysql-exporter sh
mysql -h $DB_HOST -u $DB_USER -p$DB_PASS -e "SHOW VARIABLES LIKE 'version';"
```

### High cardinality warnings

If Prometheus shows "cardinality too high" warnings:
- Avoid unbounded labels (e.g., user IDs, email addresses)
- Use histogram buckets instead of many gauges
- Limit custom tags to low-cardinality values (e.g., tenant, region)

---

## 📦 Backup & Retention

### Prometheus Data Retention

Default: 30 days (configured in `prometheus.yml`)

To change:
```yaml
command:
  - '--storage.tsdb.retention.time=90d'  # 90 days
  - '--storage.tsdb.retention.size=50GB'  # or 50GB limit
```

### Grafana Dashboard Backup

```bash
# Export all dashboards (requires API key)
grafana-backup save \
  --grafana-url http://localhost:3000 \
  --grafana-token YOUR_API_TOKEN \
  --output ./grafana-backups/

# Or manual export:
# Grafana UI → Dashboard → Settings → JSON Model → Copy
```

---

## 💰 Cost Estimate (AWS CloudWatch Alternative)

| Service          | Cost (Monthly)                  |
| ---------------- | ------------------------------- |
| EC2 t3.small     | ~$15 (shared with monitoring)   |
| EBS 20GB         | ~$2                             |
| **Total**        | **~$17/month**                  |

Compare to AWS CloudWatch custom metrics: ~$0.30/metric/month = $150-300/month for equivalent coverage.

**Savings**: ~90% cost reduction using self-hosted Prometheus/Grafana.

---

## 📚 Additional Resources

- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Prometheus Query Language (PromQL)](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Alerting Guide](https://grafana.com/docs/grafana/latest/alerting/)
- [Micrometer Registry](https://micrometer.io/docs/registry/prometheus)

---

## ✅ Production Checklist

- [ ] Monitoring stack deployed via `docker-compose.monitoring.yml`
- [ ] All Prometheus targets showing `UP` status
- [ ] Grafana dashboards imported and displaying data
- [ ] Alert rules configured and tested
- [ ] Alertmanager configured for notifications
- [ ] Grafana exposed via HTTPS with IP whitelist
- [ ] Prometheus data retention set to 30+ days
- [ ] Backup strategy for Grafana dashboards
- [ ] Team trained on dashboard usage
- [ ] On-call runbook created for alerts
