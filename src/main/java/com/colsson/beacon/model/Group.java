package com.colsson.beacon.model;

import java.util.*;

/**
 * Representa un grupo en Beacon.
 *
 * <p>Beacon no tiene lógica condicional sobre nombres de grupo.
 * Todos los grupos son tratados de forma genérica.
 *
 * <p>Un grupo puede tener permisos, padres (herencia) e hijos.
 * La detección de ciclos se ejecuta al añadir una relación de herencia.
 *
 * <p>Permisos soportan world-specific:
 * <ul>
 *   <li>world = null → global (aplica a todos los mundos)</li>
 *   <li>world = "lobby" → solo aplica en lobby</li>
 * </ul>
 */
public final class Group {

    private final long id;
    private String name;
    private int priority;
    private String description;
    private final Map<String, Map<String, PermissionAssignment>> permissions = new LinkedHashMap<>();
    private final Set<Group> parents = new LinkedHashSet<>();
    private final Set<Group> children = new LinkedHashSet<>();
    private final Set<User> members = new LinkedHashSet<>();

    public Group(long id, String name, int priority, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del grupo no puede ser nulo o vacío");
        }
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.description = description != null ? description : "";
    }

    public Group(long id, String name, int priority, String description,
                 Map<String, PermissionAssignment> permissions,
                 Set<Group> parents, Set<Group> children) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del grupo no puede ser nulo o vacío");
        }
        this.id = id;
        this.name = name;
        this.priority = priority;
        this.description = description != null ? description : "";
        // Legacy: flat map → store as global
        for (PermissionAssignment assignment : permissions.values()) {
            this.permissions
                .computeIfAbsent(null, k -> new LinkedHashMap<>())
                .put(assignment.permission(), assignment);
        }
        this.parents.addAll(parents);
        this.children.addAll(children);
    }

    public Group(long id, String name, int priority) {
        this(id, name, priority, "");
    }

    public long id() { return id; }
    public String name() { return name; }
    public int priority() { return priority; }
    public String description() { return description; }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del grupo no puede ser nulo o vacío");
        }
        this.name = name;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    // ── Permisos (backward compatible — global) ─────────────

    /**
     * Retorna vista aplanada de todos los permisos (todos los worlds).
     * Key = "world:permission" (world=null → "global:permission")
     */
    public Map<String, PermissionAssignment> permissions() {
        Map<String, PermissionAssignment> flat = new LinkedHashMap<>();
        for (var worldEntry : permissions.entrySet()) {
            String world = worldEntry.getKey();
            for (var permEntry : worldEntry.getValue().entrySet()) {
                String key = (world == null ? "global" : world) + ":" + permEntry.getKey();
                flat.put(key, permEntry.getValue());
            }
        }
        return Collections.unmodifiableMap(flat);
    }

    /**
     * Cuenta el total de permisos (todos los worlds).
     */
    public int permissionCount() {
        int count = 0;
        for (Map<String, PermissionAssignment> worldPerms : permissions.values()) {
            count += worldPerms.size();
        }
        return count;
    }

    // ── Permisos — resolución ───────────────────────────────

    /**
     * Resolución de permiso (global):
     * 1. ¿Tiene world-specific DEFINIDO en algún mundo? → NO se usa aquí
     * 2. ¿Tiene permiso global (world=null)? → Usar ese
     * 3. UNDEFINED
     */
    public PermissionState getPermissionState(String permission) {
        return getPermissionState(permission, null);
    }

    /**
     * Resolución de permiso con world:
     * 1. ¿Tiene world-specific DEFINIDO para `world`? → Usar ese
     * 2. ¿Tiene permiso global (world=null)? → Usar ese
     * 3. UNDEFINED
     */
    public PermissionState getPermissionState(String permission, String world) {
        // 1. Buscar world-specific (solo si world no es null)
        if (world != null) {
            Map<String, PermissionAssignment> worldPerms = permissions.get(world);
            if (worldPerms != null) {
                PermissionAssignment assignment = worldPerms.get(permission);
                if (assignment != null) {
                    return assignment.toState();
                }
            }
        }
        // 2. Buscar global
        Map<String, PermissionAssignment> globalPerms = permissions.get(null);
        if (globalPerms != null) {
            PermissionAssignment assignment = globalPerms.get(permission);
            if (assignment != null) {
                return assignment.toState();
            }
        }
        return PermissionState.UNDEFINED;
    }

    public boolean hasPermission(String permission) {
        return hasPermission(permission, null);
    }

    public boolean hasPermission(String permission, String world) {
        return getPermissionState(permission, world).isDefined();
    }

    // ── Permisos — escritura (backward compatible) ──────────

    /**
     * Establece un permiso global (world=null).
     */
    public void setPermission(String permission, boolean value) {
        setPermission(permission, value, null);
    }

    /**
     * Establece un permiso para un mundo específico.
     */
    public void setPermission(String permission, boolean value, String world) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("El permiso no puede ser nulo o vacío");
        }
        permissions
            .computeIfAbsent(world, k -> new LinkedHashMap<>())
            .put(permission, new PermissionAssignment(permission, value));
    }

    // ── Permisos — eliminación ──────────────────────────────

    /**
     * Elimina un permiso de TODOS los worlds.
     */
    public boolean removePermission(String permission) {
        boolean removed = false;
        for (Map<String, PermissionAssignment> worldPerms : permissions.values()) {
            if (worldPerms.remove(permission) != null) {
                removed = true;
            }
        }
        return removed;
    }

    /**
     * Elimina un permiso de un world específico.
     */
    public boolean removePermission(String permission, String world) {
        Map<String, PermissionAssignment> worldPerms = permissions.get(world);
        if (worldPerms == null) return false;
        return worldPerms.remove(permission) != null;
    }

    /**
     * Elimina todos los permisos de TODOS los worlds.
     */
    public void clearPermissions() {
        permissions.clear();
    }

    /**
     * Elimina todos los permisos de un world específico.
     */
    public void clearPermissions(String world) {
        permissions.remove(world);
    }

    // ── Utilidades ──────────────────────────────────────────

    /**
     * Retorna los worlds que tienen permisos.
     */
    public Set<String> worlds() {
        return Collections.unmodifiableSet(permissions.keySet());
    }

    /**
     * Retorna los permisos de un world específico.
     */
    public Map<String, PermissionAssignment> permissions(String world) {
        Map<String, PermissionAssignment> perms = permissions.get(world);
        return perms != null
            ? Collections.unmodifiableMap(perms)
            : Collections.emptyMap();
    }

    // ── Herencia ────────────────────────────────────────────

    /**
     * Añade un padre a este grupo (este grupo hereda del padre).
     *
     * @throws CycleDetectedException si la relación crearía un ciclo
     */
    public void addParent(Group parent) {
        if (parent == null) {
            throw new IllegalArgumentException("El padre no puede ser nulo");
        }
        if (parent.equals(this)) {
            throw new CycleDetectedException("Un grupo no puede heredar de sí mismo");
        }
        if (parents.contains(parent)) {
            return;
        }
        if (wouldCreateCycle(parent)) {
            throw new CycleDetectedException(
                "La herencia " + this.name + " → " + parent.name + " crearía un ciclo"
            );
        }
        parents.add(parent);
        parent.children.add(this);
    }

    public boolean removeParent(Group parent) {
        if (parent == null) return false;
        boolean removed = parents.remove(parent);
        if (removed) {
            parent.children.remove(this);
        }
        return removed;
    }

    public Set<Group> parents() {
        return Collections.unmodifiableSet(parents);
    }

    public Set<Group> children() {
        return Collections.unmodifiableSet(children);
    }

    /**
     * Retorna todos los ancestros de este grupo (padres, abuelos, etc.)
     * en orden de traversal en profundidad.
     */
    public Set<Group> ancestors() {
        Set<Group> ancestors = new LinkedHashSet<>();
        collectAncestors(this, ancestors);
        return ancestors;
    }

    private void collectAncestors(Group current, Set<Group> visited) {
        for (Group parent : current.parents) {
            if (visited.add(parent)) {
                collectAncestors(parent, visited);
            }
        }
    }

    /**
     * Verifica si añadir parent como padre crearía un ciclo.
     * Recorre los ancestros de parent buscando si este grupo ya es ancestro.
     */
    private boolean wouldCreateCycle(Group parent) {
        Set<Group> ancestors = new LinkedHashSet<>();
        collectAncestors(parent, ancestors);
        return ancestors.contains(this);
    }

    // ── Miembros ────────────────────────────────────────────

    Set<User> membersInternal() {
        return members;
    }

    public Set<User> members() {
        return Collections.unmodifiableSet(members);
    }

    void addMember(User user) {
        members.add(user);
    }

    void removeMember(User user) {
        members.remove(user);
    }

    // ── Identidad ───────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Group{" + name + " (id=" + id + ", priority=" + priority +
               ", parents=" + parents.size() + ", permissions=" + permissionCount() + ")}";
    }
}
