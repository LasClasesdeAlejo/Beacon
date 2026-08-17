package com.colsson.beacon.api;

import com.colsson.beacon.model.Group;
import com.colsson.beacon.model.PermissionResult;
import com.colsson.beacon.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública de Beacon.
 *
 * <p>Anvil y Loom SOLO acceden a Beacon mediante esta interfaz (R12).
 * Ningún plugin externo debe importar clases de com.colsson.beacon.*
 * fuera de com.colsson.beacon.api.*.
 */
public interface BeaconAPI {

    // ── Consultas (sin razón) ───────────────────────────────

    Optional<User> getUser(UUID uuid);

    List<User> findAllUsers();

    List<Group> getGroupsOf(UUID uuid);

    Optional<Group> getGroup(String name);

    List<Group> getGroups();

    boolean hasPermission(UUID uuid, String permission);

    PermissionResult getPermissionState(UUID uuid, String permission);

    // ── Grupos — CRUD ──────────────────────────────────────

    long createGroup(String name, int priority, String description,
                     String actor, String reason);

    void deleteGroup(String name, String actor, String reason);

    // ── Grupos — Edición ───────────────────────────────────

    void renameGroup(String oldName, String newName, String actor, String reason);

    void setGroupPriority(String name, int priority, String actor, String reason);

    void setGroupDescription(String name, String description, String actor, String reason);

    // ── Grupos — Herencia ──────────────────────────────────

    void setParent(String child, String parent, String actor, String reason);

    void removeParent(String child, String parent, String actor, String reason);

    List<Group> getParents(String groupName);

    List<Group> getAncestors(String groupName);

    // ── Grupos — Permisos ──────────────────────────────────

    void setGroupPermission(String group, String permission, boolean value,
                            String actor, String reason);

    void removeGroupPermission(String group, String permission, String actor, String reason);

    void clearGroupPermissions(String group, String actor, String reason);

    // ── Usuarios — Grupos ──────────────────────────────────

    void addUserToGroup(UUID uuid, String groupName, String actor, String reason);

    void removeUserFromGroup(UUID uuid, String groupName, String actor, String reason);

    // ── Usuarios — Permisos ────────────────────────────────

    void setUserPermission(UUID uuid, String permission, boolean value,
                           String actor, String reason);

    void removeUserPermission(UUID uuid, String permission, String actor, String reason);

    void clearUserPermissions(UUID uuid, String actor, String reason);

    // ── Recarga ────────────────────────────────────────────

    void reload();
}
