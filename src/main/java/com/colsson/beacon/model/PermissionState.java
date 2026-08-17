package com.colsson.beacon.model;

/**
 * Estados conceptuales de un permiso en Beacon.
 *
 * <p>UNDEFINED es fundamentalmente distinto de FALSE:
 * <ul>
 *   <li>TRUE     — el origen concede el permiso</li>
 *   <li>FALSE    — el origen niega explícitamente el permiso</li>
 *   <li>UNDEFINED — el origen no tiene ninguna asignación para ese permiso</li>
 * </ul>
 */
public enum PermissionState {
    TRUE,
    FALSE,
    UNDEFINED;

    public boolean isDefined() {
        return this != UNDEFINED;
    }

    public boolean isDenied() {
        return this == FALSE;
    }

    public boolean isGranted() {
        return this == TRUE;
    }
}
