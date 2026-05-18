package com.workshop.inventory;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("inventory-service");
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Item lookup(long id) {
        // SLOW_QUERY(1500) is a SQL function we registered in schema.sql that
        // sleeps for the given milliseconds, so the slowness lives inside the
        // JDBC call itself — that's what students will spot as the bottleneck.
        String sql = "SELECT id, name, stock, SLOW_QUERY(1500) AS delay_ms FROM items WHERE id = ?";
        Item item = jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new Item(rs.getLong("id"), rs.getString("name"), rs.getInt("stock")), id);

        recordAuditAsync(id);
        return item;
    }

    public void recordAuditAsync(long itemId) {
        // TODO Exercise 3 — fix propagation.
        //
        // Right now, the audit-log span below shows up as an orphan trace in
        // Grafana Cloud Traces because trace context doesn't follow the lambda
        // into the executor thread. Capture the current context before the
        // supplyAsync call (or wrap the lambda with Context.current().wrap(...))
        // so the audit-log span ends up as a child of the parent request span.
        CompletableFuture.supplyAsync(() -> {
            Span span = tracer.spanBuilder("audit-log").startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.setAttribute("audit.item.id", itemId);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            } finally {
                span.end();
            }
        }, executor);
    }
}
