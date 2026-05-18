# OpenTelemetry Tracing on Grafana Cloud — Workshop Lab

A two-service Java app for the OpenTelemetry on Grafana Cloud workshop. You'll
auto-instrument the services with the OTel Java agent, add manual business
spans, and fix a broken async context propagation bug — all visible in Grafana
Cloud Traces.

**Full instructions are in [`workshop-lab-instructions.md`](./workshop-lab-instructions.md).**

## Quick start

```bash
cp .env.example .env
# Edit .env and paste in your Grafana Cloud OTLP credentials
docker compose up -d
curl http://localhost:8080/orders/1
```

## Layout

```
.
├── docker-compose.yml
├── .env.example
├── order-service/        # Spring Boot, port 8080, calls inventory-service
└── inventory-service/    # Spring Boot, port 8081, embedded H2 with a slow JDBC path
```
