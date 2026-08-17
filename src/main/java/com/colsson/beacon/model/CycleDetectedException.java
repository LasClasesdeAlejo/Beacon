package com.colsson.beacon.model;

/**
 * Se lanza al detectar un ciclo en la jerarquía de herencia de grupos.
 */
public class CycleDetectedException extends BeaconException {

    public CycleDetectedException(String message) {
        super(message);
    }
}
