package com.colsson.beacon.model;

import java.util.*;

/**
 * Representa un jugador en Beacon.
 *
 * <p>La identidad permanente es el UUID. El nombre puede cambiarse.
 * Un usuario puede pertenecer a múltiples grupos y tener permisos directos.
 *
 * <p>Permisos directos soportan world-specific:
 * <ul>
 *   <li>world = null → global (aplica a todos los mundos)</li>
 *   <li>world = "lobby" → solo aplica en lobby</li>
 * </ul>
 *
 * <p>Resolución: world-specific DEFINIDO gana sobre global.
 * World-specific UNDEFINED no bloquea el global.
 */
public final class User {

    private final UUID uuid;
    private String username;
    private final Set<Group> groups = new LinkedHashSet<>();
    private final Map<String, Map<String, PermissionAssignment>> directPermissions = new LinkedHashMap<>();

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

    // ── Permisos directos (backward compatible — global) ─────

    /**
     * Retorna vista aplanada de todos los permisos directos (todos los worlds).
     * Key = "world:permission" (world=null → "null:permission")
     */
    public Map<String, PermissionAssignment> directPermissions() {
        Map<String, PermissionAssignment> flat = new LinkedHashMap<>();
        for (var worldEntry : directPermissions.entrySet()) {
            String world = worldEntry.getKey();
            for (var permEntry : worldEntry.getValue().entrySet()) {
                String key = (world == null ? "global" : world) + ":" + permEntry.getKey();
                flat.put(key, permEntry.getValue());
            }
        }
        return Collections.unmodifiableMap(flat);
    }

    /**
     * Cuenta el total de permisos directos (todos los worlds).
     */
    public int directPermissionCount() {
        int count = 0;
        for (Map<String, PermissionAssignment> worldPerms : directPermissions.values()) {
            count += worldPerms.size();
        }
        return count;
    }

    // ── Permisos directos — resolución ──────────────────────

    /**
     * Resolución de permiso directo (global):
     * 1. ¿Tiene world-specific DEFINIDO en algún mundo? → NO se usa aquí
     * 2. ¿Tiene permiso global (world=null)? → Usar ese
     * 3. UNDEFINED
     */
    public PermissionState getDirectPermissionState(String permission) {
        return getDirectPermissionState(permission, null);
    }

    /**
     * Resolución de permiso directo con world:
     * 1. ¿Tiene world-specific DEFINIDO para `world`? → Usar ese
     * 2. ¿Tiene permiso global (world=null)? → Usar ese
     * 3. UNDEFINED
     */
    public PermissionState getDirectPermissionState(String permission, String world) {
        // 1. Buscar world-specific (solo si world no es null)
        if (world != null) {
            Map<String, PermissionAssignment> worldPerms = directPermissions.get(world);
            if (worldPerms != null) {
                PermissionAssignment assignment = worldPerms.get(permission);
                if (assignment != null) {
                    return assignment.toState();
                }
            }
        }
        // 2. Buscar global
        Map<String, PermissionAssignment> globalPerms = directPermissions.get(null);
        if (globalPerms != null) {
            PermissionAssignment assignment = globalPerms.get(permission);
            if (assignment != null) {
                return assignment.toState();
            }
        }
        return PermissionState.UNDEFINED;
    }

    /**
     * ¿Tiene permiso directo definido en algún scope?
     */
    public boolean hasDirectPermission(String permission) {
        return hasDirectPermission(permission, null);
    }

    /**
     * ¿Tiene permiso directo definido en un scope específico?
     */
    public boolean hasDirectPermission(String permission, String world) {
        return getDirectPermissionState(permission, world).isDefined();
    }

    // ── Permisos directos — escritura (backward compatible) ──

    /**
     * Establece un permiso directo global (world=null).
     */
    public void setDirectPermission(String permission, boolean value) {
        setDirectPermission(permission, value, null);
    }

    /**
     * Establece un permiso directo para un mundo específico.
     */
    public void setDirectPermission(String permission, boolean value, String world) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("El permiso no puede ser nulo o vacío");
        }
        directPermissions
            .computeIfAbsent(world, k -> new LinkedHashMap<>())
            .put(permission, new PermissionAssignment(permission, value));
    }

    // ── Permisos directos — eliminación ─────────────────────

    /**
     * Elimina un permiso directo de TODOS los worlds.
     */
    public boolean removeDirectPermission(String permission) {
        boolean removed = false;
        for (Map<String, PermissionAssignment> worldPerms : directPermissions.values()) {
            if (worldPerms.remove(permission) != null) {
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Elimina un permiso directo de un world específico.
     */
    public boolean removeDirectPermission(String permission, String world) {
        Map<String, PermissionAssignment> worldPerms = directPermissions.get(world);
        if (worldPerms == null) return false;
        return worldPerms.remove(permission) != null;
    }

    /**
     * Elimina todos los permisos directos de TODOS los worlds.
     */
    public void clearDirectPermissions() {
        directPermissions.clear();
    }

    /**
     * Elimina todos los permisos directos de un world específico.
     */
    public void clearDirectPermissions(String world) {
        directPermissions.remove(world);
    }

    // ── Utilidades ──────────────────────────────────────────

    /**
     * Retorna los worlds que tienen permisos directos.
     */
    public Set<String> worlds() {
        return Collections.unmodifiableSet(directPermissions.keySet());
    }

    /**
     * Retorna los permisos directos de un world específico.
     */
    public Map<String, PermissionAssignment> directPermissions(String world) {
        Map<String, PermissionAssignment> perms = directPermissions.get(world);
        return perms != null
            ? Collections.unmodifiableMap(perms)
            : Collections.emptyMap();
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
               ", directPermissions=" + directPermissionCount() + "}";
    }
}
