package com.workshop.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    public void validateOrder(Order order) {
        // TODO Exercise 2 — instrument me.
        //
        // Wrap the body of this method in a manual span named "validate-order",
        // and attach the order.id and customer.tier attributes. See the lab
        // instructions for the exact pattern (tracer.spanBuilder(...).startSpan(),
        // try-with-resources on the Scope, span.end() in finally).

        if (order.getId() == null || order.getId().isBlank()) {
            throw new IllegalArgumentException("order id is required");
        }

        try {
            // Gold-tier validation is a bit slower — gives TraceQL filters
            // like { name = "validate-order" && duration > 100ms } something
            // real to find.
            Thread.sleep("gold".equals(order.getCustomerTier()) ? 150 : 50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
