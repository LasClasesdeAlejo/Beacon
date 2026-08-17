package com.colsson.beacon.model;

import java.util.Collections;
import java.util.Objects;

/**
 * Resultado de la resolución de un permiso para un usuario.
 *
 * <p>Contiene el estado final, la fuente que lo resolvió y la traza completa.
 */
public final class PermissionResult {

    private final PermissionState state;
    private final String source;
    private final String trace;

    public PermissionResult(PermissionState state, String source, String trace) {
        this.state = state;
        this.source = source;
        this.trace = trace;
    }

    public PermissionState state() {
        return state;
    }

    public String source() {
        return source;
    }

    public String trace() {
        return trace;
    }

    public boolean isGranted() {
        return state == PermissionState.TRUE;
    }

    public boolean isDenied() {
        return state == PermissionState.FALSE;
    }

    public boolean isUndefined() {
        return state == PermissionState.UNDEFINED;
    }

    public static PermissionResult undefined() {
        return new PermissionResult(PermissionState.UNDEFINED, "none", "No assignments found");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PermissionResult that)) return false;
        return state == that.state && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, source);
    }

    @Override
    public String toString() {
        return state + " (source: " + source + ")";
    }
}
