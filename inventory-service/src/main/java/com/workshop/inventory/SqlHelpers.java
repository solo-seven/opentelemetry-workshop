package com.workshop.inventory;

// Registered as a SQL function via:
//   CREATE ALIAS SLOW_QUERY FOR "com.workshop.inventory.SqlHelpers.slowQuery"
// Lets us put the simulated slowness inside the JDBC call itself, so the
// JDBC instrumentation reports a wide span — which is what students should
// identify as the bottleneck in Exercise 1.
public class SqlHelpers {

    public static int slowQuery(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return millis;
    }
}
