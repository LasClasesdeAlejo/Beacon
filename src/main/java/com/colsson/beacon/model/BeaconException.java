package com.colsson.beacon.model;

/**
 * Excepción base para errores de dominio de Beacon.
 */
public class BeaconException extends RuntimeException {

    public BeaconException(String message) {
        super(message);
    }

    public BeaconException(String message, Throwable cause) {
        super(message, cause);
    }
}
