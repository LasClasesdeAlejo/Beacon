package com.colsson.beacon.model;

/**
 * Se lanza al intentar crear un grupo con un nombre que ya existe.
 */
public class DuplicateGroupException extends BeaconException {

    public DuplicateGroupException(String groupName) {
        super("El grupo '" + groupName + "' ya existe");
    }
}
