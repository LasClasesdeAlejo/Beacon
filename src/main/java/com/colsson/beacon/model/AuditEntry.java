package com.colsson.beacon.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Entrada de auditoría que registra una modificación administrativa.
 */
public final class AuditEntry {

    private final long id;
    private final String actor;
    private final AuditAction action;
    private final String targetType;
    private final String target;
    private final String oldValue;
    private final String newValue;
    private final String reason;
    private final Instant timestamp;
    private final String server;

    public AuditEntry(long id, String actor, AuditAction action, String targetType,
                      String target, String oldValue, String newValue,
                      String reason, Instant timestamp, String server) {
        this.id = id;
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.target = target;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
        this.timestamp = timestamp;
        this.server = server;
    }

    public long id() { return id; }
    public String actor() { return actor; }
    public AuditAction action() { return action; }
    public String targetType() { return targetType; }
    public String target() { return target; }
    public String oldValue() { return oldValue; }
    public String newValue() { return newValue; }
    public String reason() { return reason; }
    public Instant timestamp() { return timestamp; }
    public String server() { return server; }

    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditEntry that)) return false;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "[" + action + "] " + targetType + ":" + target +
               " (" + oldValue + " → " + newValue + ")" +
               (hasReason() ? " reason=\"" + reason + "\"" : "");
    }
}
