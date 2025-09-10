package com.assignment.phoneinventory.constants;

/** Central SQL for audit_logs. Keep DAOs free of raw SQL strings. */
public final class AuditLogConstants {
    private AuditLogConstants() {}

    public static final String INSERT_LOG =
            "INSERT INTO audit_logs(number_id, from_state, to_state, user_id, timestamp, note) VALUES (?,?,?,?,?,?)";
}
