package com.colsson.beacon.model;

import java.util.Objects;

/**
 * Asignación concreta de un permiso con un valor booleano.
 *
 * <p>La AUSENCIA de esta asignación representa UNDEFINED.
 * Esta clase solo almacena TRUE (value=true) o FALSE (value=false).
 */
public final class PermissionAssignment {

    private final String permission;
    private final boolean value;

    public PermissionAssignment(String permission, boolean value) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("El permiso no puede ser nulo o vacío");
        }
        this.permission = permission;
        this.value = value;
    }

    public String permission() {
        return permission;
    }

    public boolean value() {
        return value;
    }

    public PermissionState toState() {
        return value ? PermissionState.TRUE : PermissionState.FALSE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionAssignment that)) return false;
        return value == that.value && Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission, value);
    }

    @Override
    public String toString() {
        return permission + "=" + (value ? "TRUE" : "FALSE");
    }
}
