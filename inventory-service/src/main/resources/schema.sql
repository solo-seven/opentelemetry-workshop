-- Map a SQL function name to our Java helper so the JDBC instrumentation
-- captures a real `db.statement` span whose duration includes the sleep.
-- This is how Exercise 1 shows up as "the bottleneck is a JDBC span".
CREATE ALIAS IF NOT EXISTS SLOW_QUERY FOR "com.workshop.inventory.SqlHelpers.slowQuery";

CREATE TABLE IF NOT EXISTS items (
    id    BIGINT PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    stock INT NOT NULL DEFAULT 0
);
