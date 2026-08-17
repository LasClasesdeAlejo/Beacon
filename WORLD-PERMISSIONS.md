# WORLD-PERMISSIONS.md — Plan de Implementación

> **Beacon v0.9.1** — Permisos por mundo/servidor
> **Objetivo:** Soporte world-specific permissions sin romper SPEC.md V1

---

## Reglas del SPEC que se PRESERVAN

| Regla | Impacto |
|---|---|
| **R5: UNDEFINED ≠ FALSE** | World-specific UNDEFINED no bloquea el global. Solo TRUE/FALSE explícitos cuentan. |
| **R6: Ciclos prohibidos** | Sin cambios. |
| **R7: Herencia explícita** | Sin cambios. Herencia es world-independiente. |
| **R8: Razones opcionales** | Razón sigue siendo opcional. World es un argumento más al final. |
| **R9: Cache behind API** | Mundo se agrega a la key de cache. |
| **R11: Consultas no auditan** | `/beacon check` con world no audita. |
| **R12: API pública** | Métodos viejos se mantienen (backward compatible). Nuevos son sobrecargas. |
| **R13: Schema versionado** | Nueva migración V8. |
| **§6 Resolución** | Orden se PRESERVA: directo → grupos → ancestros. Solo se agrega filtro world. |

---

## Ejemplo de uso

```
# Global: default tiene fly
/beacon group default permission set anvil.fly true

# Override: deshabilitar fly en skyblock
/beacon group default permission set anvil.fly false skyblock

# Resultado:
#   lobby:    default → fly = TRUE (global)
#   skyblock: default → fly = FALSE (world-specific gana)
#   survival: default → fly = TRUE (global)

# Remover override (restaura global)
/beacon group default permission remove anvil.fly skyblock

# Verificar en contexto
/beacon check Colsson anvil.fly skyblock
```

---

## Fase 9.1 — Modelo de dominio

**Archivos:** `PermissionAssignment.java`, `User.java`, `Group.java`

### PermissionAssignment.java (sin cambios)

```java
// Se mantiene igual. Es un value object inmutable.
public record PermissionAssignment(String permission, boolean value) {}
```

### User.java — cambio de estructura

```java
// ANTES (línea 16):
private final Map<String, PermissionAssignment> directPermissions = new LinkedHashMap<>();

// AHORA:
private final Map<String, Map<String, PermissionAssignment>> directPermissions = new LinkedHashMap<>();
// key = world (null = global) → permission → assignment
```

**Métodos a modificar:**

| Línea actual | Método | Cambio |
|---|---|---|
| 73 | `directPermissions()` | Retorna vista aplanada (todos los worlds) |
| 81 | `getDirectPermissionState(String)` | Busca world-specific primero, luego global |
| **NUEVO** | `getDirectPermissionState(String, String world)` | Busca en world específico + global |
| 89 | `setDirectPermission(String, boolean)` | Store con world=null |
| **NUEVO** | `setDirectPermission(String, boolean, String world)` | Store con world |
| 99 | `removeDirectPermission(String)` | Remueve de todos los worlds |
| **NUEVO** | `removeDirectPermission(String, String world)` | Remueve solo de un world |
| 106 | `clearDirectPermissions()` | Limpia todos |
| **NUEVO** | `clearDirectPermissions(String world)` | Limpia solo un world |
| 110 | `hasDirectPermission(String)` | Chequea world-specific + global |

### Group.java — mismo patrón

```java
// ANTES (línea 20):
private final Map<String, PermissionAssignment> permissions = new LinkedHashMap<>();

// AHORA:
private final Map<String, Map<String, PermissionAssignment>> permissions = new LinkedHashMap<>();
// key = world (null = global) → permission → assignment
```

**Métodos a modificar:** mismos que User (getPermissionState, setPermission, removePermission, clearPermissions).

**Tests nuevos:** ~15

---

## Fase 9.2 — PermissionResolver

**Archivo:** `PermissionResolver.java`

### Cambio de firma

```java
// ANTES (línea 31):
public PermissionResult resolve(User user, String permission)

// AHORA:
public PermissionResult resolve(User user, String permission, String world)
public PermissionResult resolve(User user, String permission) {
    return resolve(user, permission, null);  // backward compatible
}
```

### Lógica de resolución (NUEVA)

```
resolve(user, permission, world):
  1. Buscar permiso directo del usuario:
     - ¿Tiene world-specific DEFINIDO para `world`? → Usar ese
     - ¿No tiene? → Usar el global (world=null)
     - Si alguno está definido → return

  2. Para CADA grupo (ordenado por prioridad descendente):
     - ¿Grupo tiene world-specific DEFINIDO para `world`? → Usar ese
     - ¿No tiene? → Usar el global del grupo
     - Si está definido → return si es TRUE o FALSE

  3. Para CADA ancestro (misma prioridad que grupo hijo):
     - Mismo lógico que paso 2

  4. Si nada definido → UNDEFINED
```

**Regla clave:** world-specific solo se usa si está EXPLÍCITAMENTE definido. UNDEFINED world-specific = "no existe" → se ignora → se usa global.

### Métodos internos a modificar

| Línea | Método | Cambio |
|---|---|---|
| 68 | `collectDefinedPermissions(User, String)` | Agregar param `world`, buscar world-specific + global |
| 78 | `collectFromGroupAndAncestors(Group, String, List)` | Agregar param `world` |
| 89 | `addIfDefined(Group, String, List)` | Agregar param `world`, buscar world-specific + global |
| **NUEVO** | `resolveDirect(User, String, String world)` | Lógica de resolución directa |
| **NUEVO** | `resolveFromGroup(Group, String, String world)` | Lógica de resolución de grupo |

**Tests nuevos:** ~25

---

## Fase 9.3 — DB Migration V8

**Archivo:** `BeaconPlugin.java` — línea 121, `getMigrations()`

### Migración V8

```java
// V8 — World-based permissions
"""
ALTER TABLE user_permissions ADD COLUMN world VARCHAR(64) DEFAULT NULL;
""",
"""
ALTER TABLE group_permissions ADD COLUMN world VARCHAR(64) DEFAULT NULL;
""",
"""
-- Recrear UNIQUE constraints (MySQL requiere drop + add)
ALTER TABLE user_permissions DROP INDEX user_permissions_user_uuid_permission_key;
""",
"""
ALTER TABLE user_permissions ADD UNIQUE(user_uuid, permission, world);
""",
"""
ALTER TABLE group_permissions DROP INDEX group_permissions_group_id_permission_key;
""",
"""
ALTER TABLE group_permissions ADD UNIQUE(group_id, permission, world);
"""
```

### Backward compatibility

- `world = NULL` = aplica a todos los mundos
- Datos existentes quedan con `world = NULL` → comportamiento idéntico a V1
- Sin necesidad de migrar datos existentes

**Tests:** `MigrationManagerTest` — verificar V8

---

## Fase 9.4 — Repositorios

**Archivo:** `PermissionRepository.java`

### Queries a modificar (11)

#### setGroupPermission (línea 27)

```java
// ANTES:
public void setGroupPermission(long groupId, String permission, boolean value)

// AHORA:
public void setGroupPermission(long groupId, String permission, boolean value, String world)

// Query cambia:
// ANTES: INSERT INTO group_permissions (group_id, permission, value) VALUES (?, ?, ?)
// AHORA: INSERT INTO group_permissions (group_id, permission, value, world) VALUES (?, ?, ?, ?)
// (con ON DUPLICATE KEY UPDATE value = VALUES(value))
```

#### removeGroupPermission (línea 48)

```java
// ANTES: DELETE FROM group_permissions WHERE group_id = ? AND permission = ?
// AHORA: DELETE FROM group_permissions WHERE group_id = ? AND permission = ? AND world = ?
```

#### clearGroupPermissions (línea 61)

```java
// ANTES: DELETE FROM group_permissions WHERE group_id = ?
// AHORA: DELETE FROM group_permissions WHERE group_id = ? AND (world = ? OR world IS NULL)
// Si world = null → limpia todos
```

#### getGroupPermissions (línea 72)

```java
// ANTES: SELECT permission, value FROM group_permissions WHERE group_id = ?
// AHORA: SELECT permission, value, world FROM group_permissions WHERE group_id = ?
// (retorna mapa con world info)
```

#### setUserPermission (línea 93) — mismo patrón que group

#### removeUserPermission (línea 114) — mismo patrón

#### clearUserPermissions (línea 127) — mismo patrón

#### getUserPermissions (línea 139) — mismo patrón

#### findAllDistinctPermissions (línea 159) — SIN CAMBIOS (world-agnostic)

**Tests nuevos:** ~10

---

## Fase 9.5 — BeaconAPIImpl

**Archivo:** `BeaconAPIImpl.java`

### Métodos API a agregar (sobrecargas)

```java
// Lectura — backward compatible + world-aware
boolean hasPermission(UUID uuid, String permission);
boolean hasPermission(UUID uuid, String permission, String world);

PermissionResult getPermissionState(UUID uuid, String permission);
PermissionResult getPermissionState(UUID uuid, String permission, String world);

// Escritura — sobrecargas con world
void setUserPermission(UUID uuid, String perm, boolean val, String actor, String reason);
void setUserPermission(UUID uuid, String perm, boolean val, String world, String actor, String reason);

void removeUserPermission(UUID uuid, String perm, String actor, String reason);
void removeUserPermission(UUID uuid, String perm, String world, String actor, String reason);

void clearUserPermissions(UUID uuid, String actor, String reason);
void clearUserPermissions(UUID uuid, String world, String actor, String reason);

void setGroupPermission(String group, String perm, boolean val, String actor, String reason);
void setGroupPermission(String group, String perm, boolean val, String world, String actor, String reason);

void removeGroupPermission(String group, String perm, String actor, String reason);
void removeGroupPermission(String group, String perm, String world, String actor, String reason);

void clearGroupPermissions(String group, String actor, String reason);
void clearGroupPermissions(String group, String world, String actor, String reason);
```

### Loading methods a modificar

| Línea | Método | Cambio |
|---|---|---|
| 157 | `hasPermission` | Llama a resolver con world=null |
| 162 | `getPermissionState` | Llama a resolver con world |
| 345 | `setGroupPermission` | Pasa world a repository |
| 360 | `removeGroupPermission` | Pasa world a repository |
| 375 | `clearGroupPermissions` | Pasa world a repository |
| 444 | `setUserPermission` | Pasa world a repository |
| 459 | `removeUserPermission` | Pasa world a repository |
| 473 | `clearUserPermissions` | Pasa world a repository |
| 501 | `loadFullUser` | Carga permisos con world info |
| 549 | `loadGroupPermissions` | Carga permisos con world info |

### BeaconAPI.java (interfaz) — agregar sobrecargas

```java
boolean hasPermission(UUID uuid, String permission, String world);
PermissionResult getPermissionState(UUID uuid, String permission, String world);
void setUserPermission(UUID uuid, String perm, boolean val, String world, String actor, String reason);
void removeUserPermission(UUID uuid, String perm, String world, String actor, String reason);
void clearUserPermissions(UUID uuid, String world, String actor, String reason);
void setGroupPermission(String group, String perm, boolean val, String world, String actor, String reason);
void removeGroupPermission(String group, String perm, String world, String actor, String reason);
void clearGroupPermissions(String group, String world, String actor, String reason);
```

**Tests nuevos:** ~10

---

## Fase 9.6 — Cache

**Archivo:** `CacheManager.java`

### Cambio de key

```java
// ANTES (línea 160):
private String buildPermissionKey(UUID userUuid, String permission) {
    return userUuid + ":" + permission;
}

// AHORA:
private String buildPermissionKey(UUID userUuid, String permission, String world) {
    return userUuid + ":" + permission + ":" + (world == null ? "*" : world);
}
```

### Métodos a modificar

| Línea | Método | Cambio |
|---|---|---|
| 107 | `getPermissionResult(UUID, String)` | Llama a buildPermissionKey con world=null |
| **NUEVO** | `getPermissionResult(UUID, String, String)` | Key incluye world |
| 118 | `putPermissionResult(UUID, String, PermissionResult)` | Llama a buildPermissionKey con world=null |
| **NUEVO** | `putPermissionResult(UUID, String, String, PermissionResult)` | Key incluye world |
| 130 | `invalidateUserPermissions(UUID)` | Invalida TODOS los worlds (ya funciona) |
| **NUEVO** | `invalidateUserPermissions(UUID, String)` | Invalida solo un world |

**Tests nuevos:** ~8

---

## Fase 9.7 — Comandos + Tab Completion

### Comandos actualizados

#### UserCommandHandler.java (línea 153)

```java
// ANTES:
/beacon user Colsson permission set anvil.fly true
/beacon user Colsson permission remove anvil.fly
/beacon user Colsson permission clear

// AHORA:
/beacon user Colsson permission set anvil.fly true              → world=null
/beacon user Colsson permission set anvil.fly true lobby        → world="lobby"
/beacon user Colsson permission remove anvil.fly skyblock       → world="skyblock"
/beacon user Colsson permission clear                           → limpia todos
/beacon user Colsson permission clear lobby                     → limpia solo lobby
```

#### GroupCommandHandler.java (línea 255)

```java
// Mismo patrón que user
/beacon group default permission set anvil.fly true skyblock
/beacon group default permission remove anvil.fly skyblock
/beacon group default permission clear skyblock
```

#### DiagnosticCommandHandler.java

```java
// ANTES:
/beacon check Colsson anvil.fly

// AHORA:
/beacon check Colsson anvil.fly              → global
/beacon check Colsson anvil.fly lobby        → world-specific
```

### TabCompletionEngine.java

Nuevos placeholders:

| Ruta | Nuevo arg |
|---|---|
| `user X permission set perm value` | `<mundo>` (opcional) |
| `user X permission remove perm` | `<mundo>` (opcional) |
| `user X permission clear` | `<mundo>` (opcional) |
| `group X permission set perm value` | `<mundo>` (opcional) |
| `group X permission remove perm` | `<mundo>` (opcional) |
| `group X permission clear` | `<mundo>` (opcional) |
| `check X perm` | `<mundo>` (opcional) |
| `debug permission X perm` | `<mundo>` (opcional) |

**Tab completion para mundos:** usar `Supplier<List<String>>` para nombres de mundos del server.

**Tests:** actualizar TabCompletionLogicTest (~10 tests nuevos)

---

## Fase 9.8 — SPEC.md update

Agregar §6.1 "Resolución por mundo":

```markdown
## 6.1 Resolución por mundo

Cada permiso puede tener un alcance global (world=null) o world-specific.

### Regla
- World-specific solo se usa si está EXPLÍCITAMENTE definido
- World-specific UNDEFINED no bloquea el global
- World-specific TRUE/FALSE siempre gana sobre global

### Ejemplo
  default: anvil.fly = TRUE (global)
  skyblock: default anvil.fly = FALSE (world-specific)

  → lobby:    fly = TRUE (usa global)
  → skyblock: fly = FALSE (world-specific gana)
```

Actualizar §11 comandos con argumento world.

---

## Fase 9.9 — Build + Deploy + Verify

### Checklist

1. `./gradlew test` — todos los tests pasan
2. `./gradlew shadowJar` — JAR generado
3. Copiar a `plugins/` del servidor
4. Reiniciar servidor
5. Probar:
   - `/beacon group default permission set anvil.fly true` (global)
   - `/beacon check Colsson anvil.fly` → TRUE
   - `/beacon group default permission set anvil.fly false skyblock`
   - `/beacon check Colsson anvil.fly skyblock` → FALSE
   - `/beacon check Colsson anvil.fly lobby` → TRUE
   - `/beacon check Colsson anvil.fly survival` → TRUE
   - `/beacon group default permission remove anvil.fly skyblock`
   - `/beacon check Colsson anvil.fly skyblock` → TRUE (restaurado global)

---

## Resumen de impacto

| Fase | Archivos | Tests nuevos | Riesgo |
|---|---|---|---|
| 9.1 Modelo | PermissionAssignment, User, Group | ~15 | ALTO |
| 9.2 Resolver | PermissionResolver | ~25 | ALTO |
| 9.3 Migration | BeaconPlugin | ~2 | ALTO |
| 9.4 Repos | PermissionRepository | ~10 | ALTO |
| 9.5 API | BeaconAPI, BeaconAPIImpl | ~10 | ALTO |
| 9.6 Cache | CacheManager | ~8 | MEDIO |
| 9.7 Commands | 3 handlers + TabEngine | ~10 | MEDIO |
| 9.8 SPEC | SPEC.md | 0 | BAJO |
| 9.9 Build | — | 0 | BAJO |
| **Total** | **12 archivos** | **~80** | — |

### Tests totales post-fase 9

```
V1:      348 tests
Fase 9:  +80 tests
Total:   ~428 tests
```

---

## Orden de ejecución

```
9.1  → 9.2  → 9.3  → 9.4  → 9.5  → 9.6  → 9.7  → 9.8  → 9.9
```

Cada sub-fase se completa, valida con `./gradlew test`, y hace commit antes de avanzar.
