package com.colsson.beacon.api;

import com.colsson.beacon.cache.CacheManager;
import com.colsson.beacon.model.*;
import com.colsson.beacon.persistence.*;
import com.colsson.beacon.persistence.GroupRepository.GroupRecord;
import com.colsson.beacon.persistence.UserRepository.UserRecord;
import com.colsson.beacon.resolver.PermissionResolver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementación de la API pública de Beacon.
 *
 * <p>Flujo de lectura: Cache → MySQL → Cache → resultado
 * <p>Flujo de escritura: MySQL → invalidación cache → auditoría
 */
public class BeaconAPIImpl implements BeaconAPI {

    private final DatabaseManager db;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final PermissionRepository permRepo;
    private final InheritanceRepository inhRepo;
    private final AuditRepository auditRepo;
    private final PermissionResolver resolver;
    private final CacheManager cache;
    private final Logger logger;
    private String serverName = "default";

    public BeaconAPIImpl(DatabaseManager db, UserRepository userRepo,
                         GroupRepository groupRepo, PermissionRepository permRepo,
                         InheritanceRepository inhRepo, AuditRepository auditRepo,
                         PermissionResolver resolver, CacheManager cache,
                         Logger logger) {
        this.db = db;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.permRepo = permRepo;
        this.inhRepo = inhRepo;
        this.auditRepo = auditRepo;
        this.resolver = resolver;
        this.cache = cache;
        this.logger = logger;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    // ══════════════════════════════════════════════════════════
    // Consultas (sin razón)
    // ══════════════════════════════════════════════════════════

    @Override
    public Optional<User> getUser(UUID uuid) {
        Optional<User> cached = cache.getUser(uuid);
        if (cached.isPresent()) return cached;

        try {
            Optional<UserRecord> record = userRepo.findByUuid(uuid);
            if (record.isEmpty()) return Optional.empty();

            try (Connection conn = db.getConnection()) {
                User user = loadFullUser(conn, record.get());
                cache.putUser(uuid, user);
                return Optional.of(user);
            }
        } catch (SQLException e) {
            logger.severe("Error loading user " + uuid + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> getUserByName(String name) {
        try {
            Optional<UserRecord> record = userRepo.findByName(name);
            if (record.isEmpty()) return Optional.empty();
            return getUser(record.get().uuid());
        } catch (SQLException e) {
            logger.severe("Error loading user by name " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAllUsers() {
        try {
            List<UserRecord> records = userRepo.findAll();
            List<User> users = new ArrayList<>();
            try (Connection conn = db.getConnection()) {
                for (UserRecord record : records) {
                    User user = loadFullUser(conn, record);
                    cache.putUser(user.uuid(), user);
                    users.add(user);
                }
            }
            return users;
        } catch (SQLException e) {
            logger.severe("Error loading all users: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Group> getGroupsOf(UUID uuid) {
        Optional<User> user = getUser(uuid);
        return user.map(User::groups).map(List::copyOf).orElse(List.of());
    }

    @Override
    public Optional<Group> getGroup(String name) {
        Optional<Group> cached = cache.getGroup(name);
        if (cached.isPresent()) return cached;

        try {
            Optional<GroupRecord> record = groupRepo.findByName(name);
            if (record.isEmpty()) return Optional.empty();

            try (Connection conn = db.getConnection()) {
                Group group = loadFullGroup(conn, record.get(), new HashSet<>());
                cache.putGroup(name, group);
                return Optional.of(group);
            }
        } catch (SQLException e) {
            logger.severe("Error loading group " + name + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<Group> getGroups() {
        try {
            List<GroupRecord> records = groupRepo.findAll();
            List<Group> groups = new ArrayList<>();
            try (Connection conn = db.getConnection()) {
                for (GroupRecord record : records) {
                    Group group = loadFullGroup(conn, record, new HashSet<>());
                    cache.putGroup(group.name(), group);
                    groups.add(group);
                }
            }
            return groups;
        } catch (SQLException e) {
            logger.severe("Error loading all groups: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return getPermissionState(uuid, permission).isGranted();
    }

    @Override
    public PermissionResult getPermissionState(UUID uuid, String permission) {
        Optional<PermissionResult> cached = cache.getPermissionResult(uuid, permission);
        if (cached.isPresent()) return cached.get();

        Optional<User> userOpt = getUser(uuid);
        if (userOpt.isEmpty()) return PermissionResult.undefined();

        PermissionResult result = resolver.resolve(userOpt.get(), permission);
        cache.putPermissionResult(uuid, permission, result);
        return result;
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission, String world) {
        return getPermissionState(uuid, permission, world).isGranted();
    }

    @Override
    public PermissionResult getPermissionState(UUID uuid, String permission, String world) {
        Optional<PermissionResult> cached = cache.getPermissionResult(uuid, permission, world);
        if (cached.isPresent()) return cached.get();

        Optional<User> userOpt = getUser(uuid);
        if (userOpt.isEmpty()) return PermissionResult.undefined();

        PermissionResult result = resolver.resolve(userOpt.get(), permission, world);
        cache.putPermissionResult(uuid, permission, world, result);
        return result;
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — CRUD
    // ══════════════════════════════════════════════════════════

    @Override
    public long createGroup(String name, int priority, String description,
                            String actor, String reason) {
        try {
            if (groupRepo.findByName(name).isPresent()) {
                throw new IllegalArgumentException("Group '" + name + "' already exists");
            }
            long id = groupRepo.create(name, priority, description);
            audit(actor, AuditAction.GROUP_CREATE, "GROUP", name, null, name, reason);
            return id;
        } catch (SQLException e) {
            throw new RuntimeException("Error creating group: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteGroup(String name, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(name);
            groupRepo.delete(record.id());
            cache.invalidateGroup(name);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_DELETE, "GROUP", name, name, null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting group: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — Edición
    // ══════════════════════════════════════════════════════════

    @Override
    public void renameGroup(String oldName, String newName, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(oldName);
            if (groupRepo.findByName(newName).isPresent()) {
                throw new IllegalArgumentException("Group '" + newName + "' already exists");
            }
            groupRepo.updateName(record.id(), newName);
            cache.invalidateGroup(oldName);
            cache.invalidateGroup(newName);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_RENAME, "GROUP", oldName, oldName, newName, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error renaming group: " + e.getMessage(), e);
        }
    }

    @Override
    public void setGroupPriority(String name, int priority, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(name);
            groupRepo.updatePriority(record.id(), priority);
            cache.invalidateGroup(name);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_EDIT_PRIORITY, "GROUP", name,
                  String.valueOf(record.priority()), String.valueOf(priority), reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting priority: " + e.getMessage(), e);
        }
    }

    @Override
    public void setGroupDescription(String name, String description, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(name);
            groupRepo.updateDescription(record.id(), description);
            cache.invalidateGroup(name);
            audit(actor, AuditAction.GROUP_EDIT_DESCRIPTION, "GROUP", name,
                  record.description(), description, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting description: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — Herencia
    // ══════════════════════════════════════════════════════════

    @Override
    public void setParent(String child, String parent, String actor, String reason) {
        try {
            GroupRecord childRec = requireGroup(child);
            GroupRecord parentRec = requireGroup(parent);

            if (inhRepo.exists(childRec.id(), parentRec.id())) {
                throw new IllegalArgumentException(
                    "Inheritance " + child + " → " + parent + " already exists");
            }
            try (Connection conn = db.getConnection()) {
                if (detectCycle(conn, parentRec.id(), childRec.id(), new HashSet<>())) {
                    throw new IllegalArgumentException(
                        "Cycle detected: " + child + " → " + parent);
                }
            }

            inhRepo.addInheritance(childRec.id(), parentRec.id());
            cache.invalidateGroup(child);
            cache.invalidateGroup(parent);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PARENT_SET, "GROUP", child, null, parent, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting parent: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeParent(String child, String parent, String actor, String reason) {
        try {
            GroupRecord childRec = requireGroup(child);
            GroupRecord parentRec = requireGroup(parent);

            if (!inhRepo.removeInheritance(childRec.id(), parentRec.id())) {
                throw new IllegalArgumentException(
                    "Inheritance " + child + " → " + parent + " not found");
            }

            cache.invalidateGroup(child);
            cache.invalidateGroup(parent);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PARENT_REMOVE, "GROUP", child, parent, null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing parent: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Group> getParents(String groupName) {
        try {
            GroupRecord record = requireGroup(groupName);
            List<Long> parentIds = inhRepo.getParentIds(record.id());
            List<Group> parents = new ArrayList<>();
            try (Connection conn = db.getConnection()) {
                for (long parentId : parentIds) {
                    try (var ps = conn.prepareStatement(
                             "SELECT id, name, priority, description FROM `groups` WHERE id = ?")) {
                        ps.setLong(1, parentId);
                        try (var rs = ps.executeQuery()) {
                            if (rs.next()) {
                                GroupRecord rec = new GroupRecord(
                                    rs.getLong("id"), rs.getString("name"),
                                    rs.getInt("priority"), rs.getString("description"));
                                parents.add(loadFullGroup(conn, rec, new HashSet<>()));
                            }
                        }
                    }
                }
            }
            return parents;
        } catch (SQLException e) {
            logger.severe("Error getting parents: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Group> getAncestors(String groupName) {
        Optional<Group> group = getGroup(groupName);
        return group.map(g -> List.copyOf(g.ancestors())).orElse(List.of());
    }

    // ══════════════════════════════════════════════════════════
    // Grupos — Permisos
    // ══════════════════════════════════════════════════════════

    @Override
    public void setGroupPermission(String group, String permission, boolean value,
                                   String actor, String reason) {
        try {
            GroupRecord record = requireGroup(group);
            permRepo.setGroupPermission(record.id(), permission, value);
            cache.invalidateGroup(group);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PERMISSION_SET, "GROUP", group,
                  null, permission + "=" + value, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting group permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeGroupPermission(String group, String permission,
                                      String actor, String reason) {
        try {
            GroupRecord record = requireGroup(group);
            permRepo.removeGroupPermission(record.id(), permission);
            cache.invalidateGroup(group);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PERMISSION_REMOVE, "GROUP", group,
                  permission, null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing group permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void clearGroupPermissions(String group, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(group);
            permRepo.clearGroupPermissions(record.id());
            cache.invalidateGroup(group);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PERMISSION_CLEAR, "GROUP", group, "all", null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing group permissions: " + e.getMessage(), e);
        }
    }

    // ── Grupos — Permisos — world-specific ─────────────────

    @Override
    public void setGroupPermission(String group, String permission, boolean value,
                                    String world, String actor, String reason) {
        try {
            GroupRecord record = requireGroup(group);
            permRepo.setGroupPermission(record.id(), permission, value, world);
            cache.invalidateGroup(group);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PERMISSION_SET, "GROUP", group,
                  null, permission + "=" + value + (world != null ? " @" + world : ""), reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting group permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeGroupPermission(String group, String permission, String world,
                                       String actor, String reason) {
        try {
            GroupRecord record = requireGroup(group);
            permRepo.removeGroupPermission(record.id(), permission, world);
            cache.invalidateGroup(group);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.GROUP_PERMISSION_REMOVE, "GROUP", group,
                  permission + (world != null ? " @" + world : ""), null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing group permission: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Usuarios — Grupos
    // ══════════════════════════════════════════════════════════

    @Override
    public void addUserToGroup(UUID uuid, String groupName, String actor, String reason) {
        try {
            requireUserRecord(uuid);
            GroupRecord groupRec = requireGroup(groupName);

            try (Connection conn = db.getConnection();
                 var ps = conn.prepareStatement(
                     "INSERT INTO user_groups (user_uuid, group_id) VALUES (?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, groupRec.id());
                ps.executeUpdate();
            }

            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.USER_GROUP_ADD, "USER", uuid.toString(),
                  null, groupName, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error adding user to group: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeUserFromGroup(UUID uuid, String groupName, String actor, String reason) {
        try {
            requireUserRecord(uuid);
            GroupRecord groupRec = requireGroup(groupName);

            try (Connection conn = db.getConnection();
                 var ps = conn.prepareStatement(
                     "DELETE FROM user_groups WHERE user_uuid = ? AND group_id = ?")) {
                ps.setString(1, uuid.toString());
                ps.setLong(2, groupRec.id());
                ps.executeUpdate();
            }

            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            cache.invalidateGroupPermissions();
            audit(actor, AuditAction.USER_GROUP_REMOVE, "USER", uuid.toString(),
                  groupName, null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing user from group: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Usuarios — Permisos
    // ══════════════════════════════════════════════════════════

    @Override
    public void setUserPermission(UUID uuid, String permission, boolean value,
                                  String actor, String reason) {
        try {
            requireUserRecord(uuid);
            permRepo.setUserPermission(uuid, permission, value);
            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            audit(actor, AuditAction.USER_PERMISSION_SET, "USER", uuid.toString(),
                  null, permission + "=" + value, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting user permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeUserPermission(UUID uuid, String permission, String actor, String reason) {
        try {
            requireUserRecord(uuid);
            permRepo.removeUserPermission(uuid, permission);
            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            audit(actor, AuditAction.USER_PERMISSION_REMOVE, "USER", uuid.toString(),
                  permission, null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing user permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void clearUserPermissions(UUID uuid, String actor, String reason) {
        try {
            requireUserRecord(uuid);
            permRepo.clearUserPermissions(uuid);
            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            audit(actor, AuditAction.USER_PERMISSION_CLEAR, "USER", uuid.toString(),
                  "all", null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error clearing user permissions: " + e.getMessage(), e);
        }
    }

    // ── Usuarios — Permisos — world-specific ───────────────

    @Override
    public void setUserPermission(UUID uuid, String permission, boolean value,
                                   String world, String actor, String reason) {
        try {
            requireUserRecord(uuid);
            permRepo.setUserPermission(uuid, permission, value, world);
            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            audit(actor, AuditAction.USER_PERMISSION_SET, "USER", uuid.toString(),
                  null, permission + "=" + value + (world != null ? " @" + world : ""), reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error setting user permission: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeUserPermission(UUID uuid, String permission, String world,
                                      String actor, String reason) {
        try {
            requireUserRecord(uuid);
            permRepo.removeUserPermission(uuid, permission, world);
            cache.invalidateUser(uuid);
            cache.invalidateUserPermissions(uuid);
            audit(actor, AuditAction.USER_PERMISSION_REMOVE, "USER", uuid.toString(),
                  permission + (world != null ? " @" + world : ""), null, reason);
        } catch (SQLException e) {
            throw new RuntimeException("Error removing user permission: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Recarga
    // ══════════════════════════════════════════════════════════

    @Override
    public void reload() {
        cache.invalidateAll();
        logger.info("Beacon cache invalidated");
    }

    // ══════════════════════════════════════════════════════════
    // Internos — Carga de modelos completos
    // Usa una única conexión para evitar deadlocks con el pool
    // ══════════════════════════════════════════════════════════

    private User loadFullUser(Connection conn, UserRecord record) throws SQLException {
        User user = new User(record.uuid(), record.username());

        try (var ps = conn.prepareStatement(
                 "SELECT g.id, g.name, g.priority, g.description " +
                 "FROM `groups` g JOIN user_groups ug ON g.id = ug.group_id " +
                 "WHERE ug.user_uuid = ?")) {
            ps.setString(1, record.uuid().toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupRecord gr = new GroupRecord(
                        rs.getLong("id"), rs.getString("name"),
                        rs.getInt("priority"), rs.getString("description"));
                    Group group = loadFullGroup(conn, gr, new HashSet<>());
                    user.addGroup(group);
                }
            }
        }

        try (var ps = conn.prepareStatement(
                 "SELECT permission, value, world FROM user_permissions WHERE user_uuid = ?")) {
            ps.setString(1, record.uuid().toString());
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    user.setDirectPermission(rs.getString("permission"),
                                            rs.getInt("value") == 1,
                                            rs.getString("world"));
                }
            }
        }

        return user;
    }

    private Group loadFullGroup(Connection conn, GroupRecord record,
                                Set<Long> visited) throws SQLException {
        if (!visited.add(record.id())) {
            return new Group(record.id(), record.name(), record.priority(),
                             record.description());
        }

        Map<String, Map<String, PermissionAssignment>> permissions =
            loadGroupPermissionsByWorld(conn, record.id());
        Set<Group> parents = loadParents(conn, record.id(), visited);
        Set<Group> children = loadChildren(conn, record.id(), visited);

        return new Group(record.id(), record.name(), record.priority(),
                         record.description(), permissions, parents, children, true);
    }

    private Map<String, Map<String, PermissionAssignment>> loadGroupPermissionsByWorld(
            Connection conn, long groupId) throws SQLException {
        Map<String, Map<String, PermissionAssignment>> result = new LinkedHashMap<>();
        try (var ps = conn.prepareStatement(
                 "SELECT permission, value, world FROM group_permissions WHERE group_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    String perm = rs.getString("permission");
                    boolean val = rs.getInt("value") == 1;
                    String world = rs.getString("world");
                    result.computeIfAbsent(world, k -> new LinkedHashMap<>())
                          .put(perm, new PermissionAssignment(perm, val));
                }
            }
        }
        return result;
    }

    private Set<Group> loadParents(Connection conn, long groupId, Set<Long> visited)
            throws SQLException {
        Set<Group> parents = new LinkedHashSet<>();
        try (var ps = conn.prepareStatement(
                 "SELECT g.id, g.name, g.priority, g.description " +
                 "FROM `groups` g JOIN group_inheritance gi ON g.id = gi.parent_id " +
                 "WHERE gi.child_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupRecord rec = new GroupRecord(
                        rs.getLong("id"), rs.getString("name"),
                        rs.getInt("priority"), rs.getString("description"));
                    parents.add(loadFullGroup(conn, rec, visited));
                }
            }
        }
        return parents;
    }

    private Set<Group> loadChildren(Connection conn, long groupId, Set<Long> visited)
            throws SQLException {
        Set<Group> children = new LinkedHashSet<>();
        try (var ps = conn.prepareStatement(
                 "SELECT g.id, g.name, g.priority, g.description " +
                 "FROM `groups` g JOIN group_inheritance gi ON g.id = gi.child_id " +
                 "WHERE gi.parent_id = ?")) {
            ps.setLong(1, groupId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    GroupRecord rec = new GroupRecord(
                        rs.getLong("id"), rs.getString("name"),
                        rs.getInt("priority"), rs.getString("description"));
                    children.add(loadFullGroup(conn, rec, visited));
                }
            }
        }
        return children;
    }

    // ══════════════════════════════════════════════════════════
    // Internos — Helpers
    // ══════════════════════════════════════════════════════════

    private GroupRecord requireGroup(String name) throws SQLException {
        Optional<GroupRecord> record = groupRepo.findByName(name);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("Group '" + name + "' not found");
        }
        return record.get();
    }

    private UserRecord requireUserRecord(UUID uuid) throws SQLException {
        Optional<UserRecord> record = userRepo.findByUuid(uuid);
        if (record.isEmpty()) {
            throw new IllegalArgumentException("User '" + uuid + "' not found");
        }
        return record.get();
    }

    private boolean detectCycle(Connection conn, long startId, long targetId,
                                Set<Long> visited) throws SQLException {
        if (startId == targetId) return true;
        if (!visited.add(startId)) return false;

        try (var ps = conn.prepareStatement(
                 "SELECT parent_id FROM group_inheritance WHERE child_id = ?")) {
            ps.setLong(1, startId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (detectCycle(conn, rs.getLong("parent_id"), targetId, visited)) return true;
                }
            }
        }
        return false;
    }

    private void audit(String actor, AuditAction action, String targetType,
                       String target, String oldValue, String newValue, String reason) {
        try {
            auditRepo.create(actor, action, targetType, target,
                             oldValue, newValue, reason, Instant.now(), serverName);
        } catch (SQLException e) {
            logger.severe("Error writing audit: " + e.getMessage());
        }
    }
}
