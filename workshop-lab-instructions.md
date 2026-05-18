# OpenTelemetry Tracing on Grafana Cloud — Lab Instructions

This lab has **three exercises**, ~10 minutes each. By the end, you'll have generated, instrumented, and debugged distributed traces in a real two-service Java application — all sent to and viewed in **Grafana Cloud Traces**.

---

## Prerequisites

- Docker + Docker Compose installed
- Java 17+ (only if you want to build locally; otherwise Docker handles it)
- A terminal, a browser, and your favorite IDE for editing Java
- **A Grafana Cloud account** — the free tier is sufficient for this lab
- Your **OpenTelemetry connection details** from Grafana Cloud (see "Setup" below)

---

## Lab architecture

```
┌──────────────────┐       HTTP        ┌────────────────────┐
│  order-service   │ ────────────────> │ inventory-service  │
│    (port 8080)   │                   │    (port 8081)     │
└────────┬─────────┘                   └──────────┬─────────┘
         │                                        │
         │     OTLP/HTTP + basic auth             │
         └────────────────┬───────────────────────┘
                          ▼
        ┌─────────────────────────────────┐
        │   Grafana Cloud OTLP Gateway    │
        │ otlp-gateway-prod-<region>.     │
        │   grafana.net/otlp              │
        └────────────────┬────────────────┘
                         ▼
        ┌─────────────────────────────────┐
        │  Grafana Cloud Traces (Tempo)   │
        └────────────────┬────────────────┘
                         ▼
        ┌─────────────────────────────────┐
        │     Your Grafana Cloud Stack    │
        │       Explore → Traces          │
        └─────────────────────────────────┘
```

- **`order-service`** receives `GET /orders/{id}`, calls `inventory-service`, returns a response
- **`inventory-service`** simulates a database lookup with a deliberate slow path
- Both services send OTLP traces **directly to the Grafana Cloud OTLP gateway** using basic authentication
- Traces land in **Grafana Cloud Traces (Tempo)** and are viewable via your Grafana Cloud stack's **Explore → Traces** view
- **No local Tempo, Grafana, or Collector container** — everything backend-side is managed by Grafana Cloud

> **Note on production architectures:** Grafana recommends running **Grafana Alloy** (their OpenTelemetry Collector distribution) between your apps and the Cloud OTLP gateway for buffering, enrichment, sampling, and credential management. We're skipping Alloy in the lab to keep setup minimal and focus on instrumentation skills — but in real systems, this is the path you'd take.

---

## Setup — get your Grafana Cloud OTLP credentials (~3 min)

1. **Sign in** to the Grafana Cloud portal: `https://grafana.com/auth/sign-in/`
2. From your **organization Overview**, click **Launch** on your stack to open it.
3. In the left nav, go to **Connections → Add new connection**, and search for **OpenTelemetry (OTLP)**.
4. Click **OpenTelemetry (OTLP)**, then **Configure** for a generated OpenTelemetry connection.
5. Follow the on-screen flow to **generate an access policy token**. Give it a name like `otel-workshop`.
6. The page will show you three values you need:
   - `OTEL_EXPORTER_OTLP_ENDPOINT` — something like `https://otlp-gateway-prod-us-central-0.grafana.net/otlp`
   - `OTEL_EXPORTER_OTLP_HEADERS` — a string starting with `Authorization=Basic ` followed by a base64-encoded `instance_id:token`
   - `OTEL_EXPORTER_OTLP_PROTOCOL` — should be `http/protobuf`

7. **Export these in your shell** (you'll reference them from `docker-compose.yml`):

   ```bash
   export OTEL_EXPORTER_OTLP_ENDPOINT="https://otlp-gateway-prod-us-central-0.grafana.net/otlp"
   export OTEL_EXPORTER_OTLP_HEADERS="Authorization=Basic <your-base64-encoded-token>"
   export OTEL_EXPORTER_OTLP_PROTOCOL="http/protobuf"
   ```

   > ⚠️ **Keep this token secret.** It's a write key to your tracing backend. Don't commit it. The lab repo's `.gitignore` excludes a local `.env` file — use that.

---

## Lab setup (~2 min)

```bash
git clone [REPO-URL]
cd otel-tracing-workshop
cp .env.example .env
# Edit .env and paste in the three OTEL_ values above
docker compose up -d
```

Wait until both services are healthy:

```bash
docker compose ps
```

Verify:

- **order-service:** `curl http://localhost:8080/health` → `{"status":"ok"}`
- **inventory-service:** `curl http://localhost:8081/health` → `{"status":"ok"}`

Make a baseline request:

```bash
curl http://localhost:8080/orders/1
```

You should get a JSON response in ~2 seconds.

**Now open your Grafana Cloud stack → Explore → Traces.** You should see... nothing. No traces. That's correct — we haven't instrumented yet. Exercise 1 fixes that.

---

# Exercise 1 — Auto-Instrumentation (10 min)

## Goal

Get distributed traces flowing from both services into **Grafana Cloud Traces** without writing a single line of Java.

## What you'll do

Attach the OpenTelemetry Java agent to both services. The agent intercepts common libraries (Spring, JDBC, the HTTP client, etc.) and emits spans automatically — straight to Grafana Cloud.

## Steps

1. **Open `docker-compose.yml`.** Find the `order-service` block. You'll see a commented environment block:

   ```yaml
   # environment:
   #   - JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar
   #   - OTEL_SERVICE_NAME=order-service
   #   - OTEL_EXPORTER_OTLP_ENDPOINT=${OTEL_EXPORTER_OTLP_ENDPOINT}
   #   - OTEL_EXPORTER_OTLP_HEADERS=${OTEL_EXPORTER_OTLP_HEADERS}
   #   - OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
   #   - OTEL_TRACES_EXPORTER=otlp
   #   - OTEL_METRICS_EXPORTER=none
   #   - OTEL_LOGS_EXPORTER=none
   #   - OTEL_RESOURCE_ATTRIBUTES=service.namespace=workshop,service.version=1.0.0,deployment.environment=lab
   ```

2. **Uncomment it.** Do the same for `inventory-service`, but change `OTEL_SERVICE_NAME` to `inventory-service`.

   > 🔑 Note: the endpoint and headers come from your shell's exported env vars (`.env` file). Compose injects them automatically. You should not paste your token directly into `docker-compose.yml`.

3. **Restart both services:**

   ```bash
   docker compose up -d --force-recreate order-service inventory-service
   ```

4. **Send a request:**

   ```bash
   curl http://localhost:8080/orders/1
   ```

5. **Find the trace in Grafana Cloud:**
   - Open your Grafana Cloud stack
   - Navigate to **Explore** (compass icon in the left nav)
   - From the datasource dropdown at the top, select the **Tempo** datasource for your stack (often named `grafanacloud-<your-stack>-traces`)
   - Choose the **Search** query type
   - Set **Service name** to `order-service` and click **Run query**
   - You should see a recent trace — click it to open the waterfall view

   > Traces typically appear within 10–30 seconds of being emitted. If you don't see them immediately, wait and re-run the query.

## What to look for

- A trace with spans from **both** services — the trace ID links them across the HTTP call
- A clearly wider span around the inventory database query
- Span attributes the agent added for free: `http.method`, `http.route`, `db.statement`, etc.
- Resource attributes on each span: `service.name`, `service.namespace`, `deployment.environment`

## The question to answer

**Which span is the bottleneck, and roughly how long does it take?**

(Expected answer: the JDBC span inside `inventory-service`, ~1.5 seconds.)

## Why this matters

You added zero Java code and run zero observability infrastructure of your own. The agent + a few environment variables gave you a full distributed trace in a managed backend. In real systems, this is often enough to find your slowest dependencies — chatty ORMs, slow third-party APIs, N+1 queries — without touching application code.

---

# Exercise 2 — Manual Span with Business Context (10 min)

## Goal

Add a custom span around a business operation so it shows up as its own bar in the Grafana Cloud waterfall, with attributes you can filter on using **TraceQL**.

## What you'll do

In `order-service`, the method `OrderService.validateOrder(Order order)` does some business validation. Right now it's invisible in traces — it's just part of the parent HTTP span. You'll wrap it in a manual span and attach attributes.

## Steps

1. **Open** `order-service/src/main/java/.../OrderService.java`. Find the `validateOrder` method — it has a `// TODO: instrument me` comment.

2. **Get a `Tracer`.** At the top of the class:

   ```java
   private final Tracer tracer = GlobalOpenTelemetry.getTracer("order-service");
   ```

3. **Wrap the body** of `validateOrder` in a span:

   ```java
   Span span = tracer.spanBuilder("validate-order").startSpan();
   try (Scope scope = span.makeCurrent()) {
       span.setAttribute("order.id", order.getId());
       span.setAttribute("customer.tier", order.getCustomerTier());
       // ... existing validation logic stays here ...
   } catch (Exception e) {
       span.setStatus(StatusCode.ERROR, e.getMessage());
       span.recordException(e);
       throw e;
   } finally {
       span.end();
   }
   ```

   Key points:
   - `makeCurrent()` returns a `Scope` so the span is set as the active context for any child spans
   - **`try-with-resources` on the `Scope`** ensures context is restored when the block exits
   - **`span.end()` in `finally`** ensures the span is closed and exported even on exceptions

4. **Rebuild and restart:**

   ```bash
   docker compose up -d --build order-service
   ```

5. **Send a few requests with different inputs:**

   ```bash
   curl http://localhost:8080/orders/1
   curl http://localhost:8080/orders/42
   curl http://localhost:8080/orders/100
   ```

6. **In Grafana Cloud → Explore → Traces**, find a recent trace. You should now see a new `validate-order` span as a child of the HTTP span.

7. **Try filtering by attribute with TraceQL.** Switch the query type from **Search** to **TraceQL** and try:

   ```traceql
   { .customer.tier = "gold" }
   ```

   Only traces with that attribute value should appear. Try other queries:

   ```traceql
   { name = "validate-order" && duration > 100ms }
   { .order.id = "42" }
   ```

## The question to answer

**Can you find your `validate-order` span in the waterfall? Is it a child of the parent HTTP span? Can you successfully filter for it with TraceQL?**

If the span appears as a sibling or orphan, your `Scope` wasn't active when the span was created — re-check the `try-with-resources`.

## Why this matters

Auto-instrumentation knows about libraries but not your domain. Manual spans + attributes give you the dimensions you'll want to filter on six months from now: "show me all slow checkouts for gold-tier customers in the EU region." That kind of TraceQL query is impossible without business-aware attributes on your spans.

## Pitfalls to flag

- **High cardinality:** `order.id` is fine as an attribute (you'll search for specific ones), but don't make it the **span name** — span names should be low-cardinality (e.g., `validate-order`, not `validate-order-12345`).
- **PII:** never put email addresses, names, tokens, or auth headers in attributes. Once in Grafana Cloud Traces, it's subject to retention policies and harder to delete than from your own database.

---

# Exercise 3 — Fix Broken Async Context Propagation (10 min)

## Goal

Recognize the symptom of lost trace context across thread boundaries, and fix it.

## What you'll do

In `inventory-service`, there's a method that uses `CompletableFuture.supplyAsync(...)` to do background work. Right now, the work inside that async block creates a span that **shows up as an unconnected, orphan trace in Grafana Cloud**. You'll fix it so the async span properly nests under the parent.

## Steps

1. **First, see the bug.** Send a request:

   ```bash
   curl http://localhost:8080/orders/1
   ```

2. **Open Grafana Cloud → Explore → Traces → Search.** Look at the recent traces.

   You should see **two separate traces** for a single request:
   - The main request trace (`order-service` → `inventory-service`)
   - A second, orphan trace containing just an `audit-log` span with no parent

   These are the **same logical request**, but the trace context didn't survive the jump into the async thread, so they're disconnected.

3. **Open** `inventory-service/src/main/java/.../InventoryService.java`. Find the `recordAuditAsync` method — it has a `// TODO: fix propagation` comment.

   You'll see something like:

   ```java
   CompletableFuture.supplyAsync(() -> {
       Span span = tracer.spanBuilder("audit-log").startSpan();
       try (Scope scope = span.makeCurrent()) {
           // do audit work
           return null;
       } finally {
           span.end();
       }
   }, executor);
   ```

   The problem: when `supplyAsync` runs the lambda on a different thread, OpenTelemetry's current context **doesn't follow**. The new span has no parent because there's no active span on that thread.

4. **Fix it** by capturing the current context and wrapping the lambda:

   ```java
   Context parentContext = Context.current();
   CompletableFuture.supplyAsync(() -> {
       try (Scope scope = parentContext.makeCurrent()) {
           Span span = tracer.spanBuilder("audit-log").startSpan();
           try (Scope spanScope = span.makeCurrent()) {
               // do audit work
               return null;
           } finally {
               span.end();
           }
       }
   }, executor);
   ```

   Or, more idiomatically, wrap the runnable:

   ```java
   CompletableFuture.supplyAsync(Context.current().wrap(() -> {
       Span span = tracer.spanBuilder("audit-log").startSpan();
       try (Scope scope = span.makeCurrent()) {
           // do audit work
           return null;
       } finally {
           span.end();
       }
   }), executor);
   ```

5. **Rebuild and restart:**

   ```bash
   docker compose up -d --build inventory-service
   ```

6. **Send a new request and check Grafana Cloud.** The `audit-log` span should now appear as a child of the parent trace — one unified waterfall, no orphans.

## The question to answer

**Is the `audit-log` span now part of the parent trace? If you click the parent trace, do you see all four spans in one waterfall in Grafana Cloud?**

## Why this matters

This is the single most common reason traces "break" in real systems. The same problem appears in:

- **Thread pools / `ExecutorService`** — same fix, wrap the `Runnable`
- **Reactive frameworks** (Reactor, RxJava) — usually have OTel integration libraries; check the docs
- **Kafka / message queues** — context must be injected into headers on send and extracted on receive (auto-instrumentation usually handles this, but custom serializers can break it)
- **Scheduled jobs** — typically start fresh traces; that's often the right behavior

Once you've seen and fixed the symptom once, you'll recognize it instantly in production. That's the whole point of doing it hands-on.

---

# Wrap-up

You've now:

1. Generated distributed traces with zero code, sent directly to Grafana Cloud Traces (auto-instrumentation)
2. Added business context with manual spans + attributes, filtered them in **TraceQL**
3. Diagnosed and fixed broken async context propagation

Keep this repo. Try adding a third service, breaking propagation in different ways, or — for production realism — drop **Grafana Alloy** between your services and Grafana Cloud and explore tail-based sampling.

## Quick reference card

```java
// Get a tracer
Tracer tracer = GlobalOpenTelemetry.getTracer("my-service");

// Create a span
Span span = tracer.spanBuilder("operation-name").startSpan();
try (Scope scope = span.makeCurrent()) {
    span.setAttribute("key", "value");
    // ... do work ...
} catch (Exception e) {
    span.setStatus(StatusCode.ERROR, e.getMessage());
    span.recordException(e);
    throw e;
} finally {
    span.end();
}

// Propagate context across threads
Runnable wrapped = Context.current().wrap(myRunnable);
executor.submit(wrapped);
```

## Environment variables for Grafana Cloud

| Variable | Purpose | Example |
|---|---|---|
| `OTEL_SERVICE_NAME` | Identifies the service in traces | `order-service` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Grafana Cloud OTLP gateway | `https://otlp-gateway-prod-us-central-0.grafana.net/otlp` |
| `OTEL_EXPORTER_OTLP_HEADERS` | Basic auth header for Grafana Cloud | `Authorization=Basic <base64(instance-id:token)>` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | Wire protocol | `http/protobuf` |
| `OTEL_TRACES_EXPORTER` | Usually `otlp` | `otlp` |
| `OTEL_METRICS_EXPORTER` | Set to `none` if you only want traces | `none` |
| `OTEL_LOGS_EXPORTER` | Set to `none` if you only want traces | `none` |
| `OTEL_RESOURCE_ATTRIBUTES` | Extra metadata on every span | `service.namespace=workshop,deployment.environment=lab` |
| `OTEL_TRACES_SAMPLER` | Sampling strategy | `parentbased_traceidratio` |
| `OTEL_TRACES_SAMPLER_ARG` | Sampling rate | `0.1` (10%) |

## Useful TraceQL snippets

```traceql
# Find slow traces
{ duration > 1s }

# Find errors in a specific service
{ resource.service.name = "order-service" && status = error }

# Find traces touching a specific customer tier
{ .customer.tier = "gold" }

# Find a specific span by name across all traces
{ name = "validate-order" }

# Combine: slow validation for gold customers
{ name = "validate-order" && .customer.tier = "gold" && duration > 500ms }
```

## Where to go next

- **Grafana Application Observability** — turnkey APM on top of the data you're already sending. Worth a look in your Cloud stack once traces are flowing.
- **Grafana Alloy** — drop it between your services and Grafana Cloud for buffering, tail sampling, k8s metadata enrichment, and centralized credentials.
- **Log/trace correlation** — inject trace IDs into your log lines (the OTel Logback appender does this automatically) and you'll be able to jump from a Loki log entry to its trace in Grafana Cloud with one click.
- **Service Graphs** — Grafana Cloud Traces auto-generates service dependency maps from trace data. Once you have multiple services reporting, check the **Service Graph** view in Explore.
