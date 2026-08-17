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
 */
public final class Group {

    private final long id;
    private String name;
    private int priority;
    private String description;
    private final Map<String, PermissionAssignment> permissions = new LinkedHashMap<>();
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
        this.permissions.putAll(permissions);
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

    // ── Permisos ────────────────────────────────────────────

    public Map<String, PermissionAssignment> permissions() {
        return Collections.unmodifiableMap(permissions);
    }

    public PermissionState getPermissionState(String permission) {
        PermissionAssignment assignment = permissions.get(permission);
        return assignment != null ? assignment.toState() : PermissionState.UNDEFINED;
    }

    public void setPermission(String permission, boolean value) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("El permiso no puede ser nulo o vacío");
        }
        permissions.put(permission, new PermissionAssignment(permission, value));
    }

    public boolean removePermission(String permission) {
        return permissions.remove(permission) != null;
    }

    public void clearPermissions() {
        permissions.clear();
    }

    public boolean hasPermission(String permission) {
        return permissions.containsKey(permission);
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
               ", parents=" + parents.size() + ", permissions=" + permissions.size() + ")}";
    }
}
