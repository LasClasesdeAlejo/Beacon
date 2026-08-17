# Beacon — Especificación V1

> Documento viviente. Toda decisión de desarrollo se validan contra este archivo.

---

## 1. Identidad

| Campo | Valor |
|---|---|
| Nombre | Beacon |
| Tipo | Plugin Paper |
| Grupo | `com.colsson` |
| Paquete base | `com.colsson.beacon` |
| Persistencia | MySQL (fuente de verdad) |
| Caché | Local por servidor |
| API | Pública, para consumo de Anvil y Loom |

---

## 2. Alcance

### Beacon ES responsable de

- Usuarios (UUID como identidad)
- Grupos
- Permisos (asignación y resolución)
- Herencia entre grupos
- Prioridades numéricas
- Resolución de conflictos
- Caché de aceleración
- Persistencia MySQL
- Auditoría de modificaciones
- API pública
- Comandos administrativos
- Diagnóstico

### Beacon NO ES responsable de

- Comandos de gameplay (`/fly`, `/kick`, `/home`, `/tpa`, `/spawn`, `/msg`, etc.)
- Presentación visual (TAB, nametag, chat, colores, prefijos, sufijos)
- Diseño visual de rangos

Gameplay → **Anvil**. Presentación → **Loom**.

---

## 3. Reglas de diseño

### R1 — Sin especialización de grupos

Beacon no tiene lógica condicional sobre nombres de grupos.

```
IF group == "STAFF"    → PROHIBIDO
IF group == "VIP"      → PROHIBIDO
```

Todos los grupos son tratados de forma genérica.

### R2 — UUID como identidad

El nombre de Minecraft es volátil. La identidad permanente es el UUID.

### R3 — Sin primaryGroup

No existe concepto de "grupo principal". Un usuario tiene N grupos.

### R4 — Sin group set

No existe `/beacon user X group set Y`. Se utilizan `group add` y `group remove`.

### R5 — UNDEFINED ≠ FALSE

```text
UNDEFINED = el origen no tiene asignación para ese permiso
FALSE     = el origen niega explícitamente ese permiso
```

Son estados fundamentalmente diferentes.

### R6 — Herencia = permisos heredados

La herencia implica ÚNICAMENTE que un grupo recibe los permisos de sus ancestros.

No implica: rangos, colores, prefijos, sufijos, jerarquía visual, ni nada más.

### R7 — Ciclos prohibidos

```
A → B → C → A    → PROHIBIDO
A → A             → PROHIBIDO
```

Beacon detecta el ciclo ANTES de guardar la relación.

### R8 — Razón opcional

Toda operación de modificación puede recibir una razón.

Las operaciones de consulta NO reciben razón.

La razón NO modifica la lógica de autorización. Solo pertenece al registro de auditoría.

### R9 — Eliminación = restauración natural

Eliminar una excepción directa de usuario restaura la resolución heredada. No se almacena un valor artificial de restauración.

### R10 — Renombrado conserva identidad

Renombrar un grupo cambia el campo `name`. Conserva: usuarios, permisos, herencia, prioridad, descripción, identidad interna, historial.

### R11 — Separación de capas

```
COMANDO → SERVICIO → DOMINIO → REPOSITORIO/CACHE → MYSQL
```

Nunca:
- Comando → SQL directo
- Plugin externo → clases internas de Beacon
- Plugin externo → SQL de Beacon

### R12 — API pública como único punto de contacto

Anvil y Loom SOLO acceden a Beacon mediante `BeaconAPI`.

---

## 4. Modelo de dominio

### 4.1 Entidades

```text
User
Group
PermissionAssignment
GroupInheritance
AuditEntry
```

### 4.2 User

```text
User
├── uuid: UUID          (identidad primaria, inmutable)
├── username: String    (actualizable)
├── groups: Set<Group>
└── directPermissions: Map<String, PermissionAssignment>
```

Reglas:
- UUID es la identidad primaria
- El nombre puede cambiar (rename en Minecraft)
- Puede tener múltiples grupos
- Permisos directos tienen prioridad sobre permisos de grupo

### 4.3 Group

```text
Group
├── id: long            (identificador interno estable)
├── name: String        (cambiable via rename)
├── priority: int
├── description: String
├── permissions: Map<String, PermissionAssignment>
├── parents: Set<Group>
└── children: Set<Group>
```

Reglas:
- `id` es estable (sobrevive renombrado)
- `name` es el nombre visible
- `priority` es numérica, configurada por admin
- No existe un conjunto fijo de grupos

### 4.4 PermissionAssignment

```text
PermissionAssignment
├── permission: String  (nodo como "anvil.fly")
└── value: boolean      (true = concede, false = niega)
```

La AUSENCIA de una asignación = `UNDEFINED`.

### 4.5 GroupInheritance

```text
GroupInheritance
├── child: Group
└── parent: Group
```

### 4.6 AuditEntry

```text
AuditEntry
├── id: long
├── actor: String        (quien ejecuta la acción)
├── action: String       (tipo de acción)
├── targetType: String   (USER, GROUP, PERMISSION, etc.)
├── target: String       (identificador del objetivo)
├── oldValue: String
├── newValue: String
├── reason: String       (nullable)
├── timestamp: Instant
└── server: String       (nombre del servidor)
```

---

## 5. Estados de permiso

```text
TRUE     → el origen concede el permiso
FALSE    → el origen niega explícitamente el permiso
UNDEFINED → el origen no tiene ninguna asignación para ese permiso
```

### Comportamiento

| Asignación | Resultado conceptual |
|---|---|
| `permission = TRUE` | `TRUE` |
| `permission = FALSE` | `FALSE` |
| Sin asignación | `UNDEFINED` |

### Regla de ausencia

La ausencia de una fila en `group_permissions` o `user_permissions` representa `UNDEFINED`.

---

## 6. Resolución de permisos

### Orden de resolución

```text
1. Permisos directos del usuario
2. Grupos directos del usuario (ordenados por prioridad descendente)
3. Ancestros de cada grupo (misma prioridad que el grupo hijo)
4. Conflicto entre grupos: gana mayor prioridad
5. UNDEFINED nunca compite como negación
6. Resultado final
```

### Reglas de conflicto

| Grupo A | Grupo B | Prioridad | Resultado |
|---|---|---|---|
| TRUE | FALSE | A > B | TRUE |
| TRUE | FALSE | B > A | FALSE |
| TRUE | UNDEFINED | cualquiera | TRUE |
| FALSE | UNDEFINED | cualquiera | FALSE |
| UNDEFINED | UNDEFINED | cualquiera | UNDEFINED |

### Excepción directa de usuario

La excepción directa del usuario tiene **máxima prioridad**, por encima de cualquier grupo.

```text
MVP++ → anvil.fly = TRUE
Colsson → anvil.fly = FALSE (excepción directa)
Resultado: FALSE
```

### Restauración por eliminación

```text
MVP++ → anvil.fly = TRUE
Colsson → anvil.fly = FALSE

/beacon user Colsson permission remove anvil.fly

Resultado: TRUE (resolución natural restaurada)
```

### permission remove vs permission set false

```text
permission set anvil.fly false  → almacena FALSE
permission remove anvil.fly     → elimina asignación → UNDEFINED
```

---

## 7. Múltiples grupos

Un usuario puede pertenecer a varios grupos simultáneamente.

```text
Colsson
├── MVP++
└── MOD
```

Ambos grupos participan en la resolución. La prioridad determina quién gana en conflictos.

---

## 8. Herencia

### Representación

```text
MVP++ → MVP+ → MVP
```

Significa: `MVP++` hereda de `MVP+`, que hereda de `MVP`.

### Alcance

Un grupo recibe los permisos definidos por sus grupos ancestros.

No hay propagación de: prioridad, nombre, descripción, ni ningún otro atributo.

### Detección de ciclos

Antes de crear la relación `child → parent`, Beacon verifica que no se forma un ciclo en el grafo.

---

## 9. Persistencia

### MySQL como fuente de verdad

```text
MySQL = verdad
Cache = aceleración
```

### Tablas

```text
users
groups
group_permissions
user_permissions
group_inheritance
audit_log
schema_migrations
```

### Migraciones

Sistema versionado. Cada versión de esquema se identifica en `schema_migrations`.

No se depende de crear manualmente tablas.

### Transacciones

Las modificaciones pasan por MySQL primero, luego se actualiza la cache.

---

## 10. Caché

### Flujo de lectura

```text
Jugador → API → Cache → resultado
                         ↓ (miss)
                       MySQL → cache → resultado
```

### Flujo de escritura

```text
Comando → Validación → MySQL → invalidación de cache → auditoría
```

### Comportamiento

- Lecturas frecuentes se ejecutan en memoria
- Escrituras siempre persisten en MySQL primero
- La cache es invalidada al modificar datos
- La cache se puede deshabilitar en config

---

## 11. Comandos

### 11.1 Generales

```text
/beacon
/beacon help
/beacon info
/beacon reload
```

### 11.2 Usuario — Consultas (sin razón)

```text
/beacon user <usuario> info
/beacon user <usuario> groups
/beacon user <usuario> permissions
```

### 11.3 Usuario — Modificaciones (razón opcional)

```text
/beacon user <usuario> group add <grupo> ["razón"]
/beacon user <usuario> group remove <grupo> ["razón"]
/beacon user <usuario> permission set <permiso> <true|false> ["razón"]
/beacon user <usuario> permission remove <permiso> ["razón"]
/beacon user <usuario> permission clear ["razón"]
```

### 11.4 Grupo — Gestión

```text
/beacon group list
/beacon group create <grupo> ["razón"]
/beacon group <grupo> info
/beacon group <grupo> delete ["razón"]
```

### 11.5 Grupo — Edición (razón opcional)

```text
/beacon group <grupo> edit name <nuevoNombre> ["razón"]
/beacon group <grupo> edit priority <numero> ["razón"]
/beacon group <grupo> edit description <texto> ["razón"]
```

### 11.6 Grupo — Herencia (razón opcional)

```text
/beacon group <grupo> parents
/beacon group <grupo> parent set <padre> ["razón"]
/beacon group <grupo> parent remove <padre> ["razón"]
```

### 11.7 Grupo — Permisos (razón opcional)

```text
/beacon group <grupo> permissions
/beacon group <grupo> permission set <permiso> <true|false> ["razón"]
/beacon group <grupo> permission remove <permiso> ["razón"]
/beacon group <grupo> permission clear ["razón"]
```

### 11.8 Permisos globales (consulta)

```text
/beacon permission list
/beacon permission info <permiso>
/beacon permission search <texto>
```

### 11.9 Diagnóstico

```text
/beacon check <usuario> <permiso>
/beacon groups tree
/beacon debug
/beacon debug user <usuario>
/beacon debug permission <usuario> <permiso>
```

### 11.10 Historial (consulta)

```text
/beacon history
/beacon history user <usuario>
/beacon history group <grupo>
/beacon history permission <permiso>
```

### Regla de comandos

| Tipo | Recibe razón |
|---|---|
| Consulta (info, list, groups, permissions, parents, check, debug, tree, history, search) | No |
| Modificación (create, delete, add, remove, set, clear, edit, parent set, parent remove) | Sí (opcional) |

---

## 12. Auditoría

### Campos de una entrada

```text
id, actor, action, targetType, target, oldValue, newValue, reason, timestamp, server
```

### Acciones que generan auditoría

- group create / delete / rename
- group edit (priority, description)
- group parent set / remove
- group permission set / remove / clear
- user group add / remove
- user permission set / remove / clear

### Acciones que NO generan auditoría

- Todas las consultas (info, list, groups, permissions, parents, check, debug, tree, history, search)

---

## 13. API pública

### Paquete

```text
com.colsson.beacon.api
```

### Métodos

```text
getUser(UUID) → Optional<User>
getGroups(UUID) → List<Group>
getGroup(String) → Optional<Group>
hasPermission(UUID, String) → boolean
getPermissionState(UUID, String) → PermissionResult
```

### Eventos

```text
GroupChangedEvent
PermissionChangedEvent
UserGroupChangedEvent
```

### Regla

Ningún plugin externo debe importar clases de `com.colsson.beacon.*` fuera de `com.colsson.beacon.api.*`.

---

## 14. Separación de plugins

| Plugin | Responsabilidad | Consumo |
|---|---|---|
| Beacon | Permisos, grupos, herencia, prioridad, resolución, auditoría, API | — |
| Anvil | Gameplay (fly, kick, home, tpa, etc.) | `BeaconAPI` |
| Loom | Presentación (TAB, nametag, chat, prefijos, sufijos, colores) | `BeaconAPI` |

---

## 15. Network

```text
                    MySQL
                      │
        ┌─────────────┼─────────────┐
        ↓             ↓             ↓
      Lobby        Survival       SkyBlock
        │             │             │
      Beacon        Beacon        Beacon
        │             │             │
      Cache         Cache         Cache
```

- Todos los servidores apuntan al mismo MySQL
- No se mantienen configuraciones independientes
- V1: MySQL + cache local es suficiente (sin Redis)

---

## 16. Criterios de finalización V1

- [ ] No existen ciclos de herencia
- [ ] No existe `primaryGroup`
- [ ] No existe `group set`
- [ ] `UNDEFINED` es diferente de `FALSE`
- [ ] Excepciones directas funcionan
- [ ] Prioridades funcionan
- [ ] Herencia funciona
- [ ] Múltiples grupos funcionan
- [ ] MySQL persiste correctamente
- [ ] Cache funciona
- [ ] Auditoría registra todo
- [ ] Razones son opcionales
- [ ] Consultas no generan auditoría
- [ ] Modificaciones generan auditoría
- [ ] Renombrar conserva el grupo
- [ ] Eliminar excepción restaura resolución natural
- [ ] Anvil consume API correctamente
- [ ] Loom consume API correctamente
- [ ] Beacon no implementa gameplay
- [ ] Beacon no implementa presentación

---

## 17. Decisiones de arquitectura

### Por qué `parent set` en vez de herencia automática

Permite al admin controlar explícitamente la jerarquía. No se asume una estructura jerárquica predeterminada.

### Por qué no `primaryGroup`

Un usuario puede ser VIP y MOD simultáneamente. Forzar un "grupo principal" crea ambigüedad. La presentación visual es responsabilidad de Loom.

### Por qué no `group set`

`group set` implica reemplazar todos los grupos. Con múltiples grupos, la operación correcta es `add`/`remove`.

### Por qué UUID como identidad

Los nombres de Minecraft cambian (renames). El UUID es permanente y único.

### Por qué la excepción directa tiene máxima prioridad

Permite excepciones individuales sin modificar la configuración del grupo. Es la forma más granular de control.

### Por qué UNDEFINED no es FALSE

Permite que un permiso "filtre" a través de capas sin ser bloqueado artificialmente. Si un grupo no define un permiso, no debería impedir que otro grupo lo conceda.
