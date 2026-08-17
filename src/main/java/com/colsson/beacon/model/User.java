package com.colsson.beacon.model;

import java.util.*;

/**
 * Representa un jugador en Beacon.
 *
 * <p>La identidad permanente es el UUID. El nombre puede cambiarse.
 * Un usuario puede pertenecer a múltiples grupos y tener permisos directos.
 */
public final class User {

    private final UUID uuid;
    private String username;
    private final Set<Group> groups = new LinkedHashSet<>();
    private final Map<String, PermissionAssignment> directPermissions = new LinkedHashMap<>();

    public User(UUID uuid, String username) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID no puede ser nulo");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username no puede ser nulo o vacío");
        }
        this.uuid = uuid;
        this.username = username;
    }

    public UUID uuid() {
        return uuid;
    }

    public String username() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username no puede ser nulo o vacío");
        }
        this.username = username;
    }

    // ── Grupos ──────────────────────────────────────────────

    public Set<Group> groups() {
        return Collections.unmodifiableSet(groups);
    }

    public boolean hasGroup(Group group) {
        return groups.contains(group);
    }

    public void addGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("El grupo no puede ser nulo");
        }
        groups.add(group);
        group.addMember(this);
    }

    public boolean removeGroup(Group group) {
        if (group == null) return false;
        boolean removed = groups.remove(group);
        if (removed) {
            group.removeMember(this);
        }
        return removed;
    }

    // ── Permisos directos ───────────────────────────────────

    public Map<String, PermissionAssignment> directPermissions() {
        return Collections.unmodifiableMap(directPermissions);
    }

    /**
     * Obtiene el estado de un permiso directo.
     * Retorna UNDEFINED si no hay asignación directa.
     */
    public PermissionState getDirectPermissionState(String permission) {
        PermissionAssignment assignment = directPermissions.get(permission);
        return assignment != null ? assignment.toState() : PermissionState.UNDEFINED;
    }

    /**
     * Establece un permiso directo (TRUE o FALSE).
     */
    public void setDirectPermission(String permission, boolean value) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("El permiso no puede ser nulo o vacío");
        }
        directPermissions.put(permission, new PermissionAssignment(permission, value));
    }

    /**
     * Elimina un permiso directo, restaurando UNDEFINED.
     */
    public boolean removeDirectPermission(String permission) {
        return directPermissions.remove(permission) != null;
    }

    /**
     * Elimina todos los permisos directos.
     */
    public void clearDirectPermissions() {
        directPermissions.clear();
    }

    public boolean hasDirectPermission(String permission) {
        return directPermissions.containsKey(permission);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "User{" + username + "/" + uuid + ", groups=" + groups.size() +
               ", directPermissions=" + directPermissions.size() + "}";
    }
}
