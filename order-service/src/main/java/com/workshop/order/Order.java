package com.workshop.order;

public class Order {

    private final String id;
    private final String customerTier;

    public Order(String id) {
        this.id = id;
        this.customerTier = deriveTier(id);
    }

    public String getId() {
        return id;
    }

    public String getCustomerTier() {
        return customerTier;
    }

    // Deterministic tier so the same order id always lands in the same tier,
    // which makes TraceQL filter examples like { .customer.tier = "gold" } reproducible.
    private static String deriveTier(String id) {
        try {
            long n = Long.parseLong(id);
            if (n % 7 == 0) return "gold";
            if (n % 5 == 0) return "silver";
            return "bronze";
        } catch (NumberFormatException e) {
            return "bronze";
        }
    }
}
