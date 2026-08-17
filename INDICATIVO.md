# INDICATIVO — Desarrollo de Anvil (Gameplay Plugin)

> Plugin de gameplay para Colsson Network. Consume **BeaconAPI** para permisos.
> Cada fase se completa, valida y taggea antes de avanzar.

---

## Convenciones

| Concepto | Regla |
|---|---|
| **SPEC.md** | Documento vivo, fuente de verdad. Todo código se valida contra él. |
| **Arquitectura** | `COMANDO → LÓGICA → BEACON API (permisos) → PAPER API` |
| **Tests** | Unitarios por fase. 0 failures para avanzar. |
| **Commits** | Mensajes en español. Un commit por fase. |
| **Tags** | `v{fase}.{incremental}-fase{N}` |
| **Build** | Gradle, Java 21, JUnit 5, Shadow JAR |
| **Paquete** | `com.colsson.anvil` |

---

## Identidad del Plugin

| Campo | Valor |
|---|---|
| Nombre | Anvil |
| Paquete base | `com.colsson.anvil` |
| Dependencia | Beacon (API) — **obligatorio** |
| Persistencia | MySQL solo para homes. Permisos via Beacon. |
| Servidor | `/Users/joseprieto/Desktop/Servidor MC/` |
| MySQL | Host: localhost, Port: 3306, User: plugins, Password: root |
| Paper | 26.2 (Paper 26.2-112) |
| Java | 21 (compile), 25 (runtime) |

---

## Alcance

### Anvil ES responsable de

- Comandos de gameplay (fly, home, tpa, spawn)
- Teletransporte entre mundos
- Gestión de hogares (home, sethome, delhome, homes)
- Invitaciones TPA (tpa, tpahere, accept, deny)
- Spawn del servidor
- Moderación (kick, ban, mute)
- Utilidades (gamemode, heal, feed, speed, vanish)

### Anvil NO ES responsable de

- Permisos y grupos → **Beacon**
- Presentación (TAB, nametag, chat) → **Loom**
- Economía → **Otro plugin**
- Jobs/clases → **Otro plugin**

---

## Permisos (via BeaconAPI)

| Permiso | Descripción | Default |
|---|---|---|
| `anvil.fly` | Activar/desactivar vuelo propio | false |
| `anvil.fly.other` | Activar/desactivar vuelo de otro jugador | false |
| `anvil.home` | Teletransportarse a un home propio | true |
| `anvil.home.set` | Establecer un home nuevo | true |
| `anvil.home.delete` | Eliminar un home propio | true |
| `anvil.home.max.<n>` | Límite de homes por grupo (ej: `anvil.home.max.5`) | 3 |
| `anvil.tpa` | Enviar solicitud de teleportación a otro jugador | true |
| `anvil.tpahere` | Enviar solicitud para que otro venga a ti | true |
| `anvil.tpa.admin` | TPA sin confirmación del otro jugador | false |
| `anvil.spawn` | Teletransportarse al spawn | true |
| `anvil.spawn.admin` | Establecer la ubicación del spawn | false |
| `anvil.kick` | Expulsar jugadores del servidor | false |
| `anvil.kick.exempt` | Inmune a ser expulsado | false |
| `anvil.ban` | Banear jugadores | false |
| `anvil.ban.temp` | Banear temporalmente (con días) | false |
| `anvil.ban.exempt` | Inmune a ser baneado | false |
| `anvil.mute` | Silenciar jugadores (no pueden escribir en chat) | false |
| `anvil.mute.exempt` | Inmune a ser silenciado | false |
| `anvil.gamemode` | Cambiar gamemode propio | false |
| `anvil.gamemode.other` | Cambiar gamemode de otro jugador | false |
| `anvil.heal` | Curarse (restablecer vida + saturación) | false |
| `anvil.heal.other` | Curar a otro jugador | false |
| `anvil.feed` | Saciar hambre propia | false |
| `anvil.feed.other` | Saciar hambre de otro jugador | false |
| `anvil.speed` | Cambiar velocidad de movimiento | false |
| `anvil.vanish` | Volverse invisible (mobs no te ven, jugadores tampoco) | false |
| `anvil.vanish.other` | Invisibilizar a otro jugador | false |

---

## Comandos Detallados

### /fly — Vuelo

**Activar/desactivar vuelo.** Cuando está activo, el jugador flota y no recibe daño por caída.

| Comando | Descripción | Permiso requerido |
|---|---|---|
| `/fly` | Activa o desactiva tu vuelo | `anvil.fly` |
| `/fly <jugador>` | Activa o desactiva el vuelo de otro | `anvil.fly.other` |

**Comportamiento:**
- Si el jugador NO tiene fly → se activa, recibe "Vuelo activado"
- Si el jugador SÍ tiene fly → se desactiva, recibe "Vuelo desactivado"
- Al desactivar, se restablece la gravedad normal
- Si se usa `/fly <jugador>`, el otro jugador recibe notificación
- **World-aware**: se verifica el permiso en el mundo actual del jugador

**Mensajes:**
```
§aVuelo activado.
§cVuelo desactivado.
§aVuelo activado para <jugador>.
§cVuelo desactivado para <jugador>.
§cNo tenés permiso para esto.
```

---

### /home — Hogares

**Gestionar puntos de teletransporte personales.** Cada jugador tiene sus propios homes guardados en MySQL.

| Comando | Descripción | Permiso |
|---|---|---|
| `/home` | Ir a tu home "default" | `anvil.home` |
| `/home <nombre>` | Ir a un home específico | `anvil.home` |
| `/sethome` | Establecer home "default" en tu ubicación | `anvil.home.set` |
| `/sethome <nombre>` | Establecer home con nombre | `anvil.home.set` |
| `/delhome <nombre>` | Eliminar un home | `anvil.home.delete` |
| `/homes` | Listar todos tus homes | `anvil.home` |

**Comportamiento:**
- `/home` sin nombre busca el home llamado "default"
- `/sethome` guarda: mundo, x, y, z, yaw, pitch del jugador actual
- Si ya existe un home con ese nombre → sobreescribe
- Límite de homes:取决于 el permiso `anvil.home.max.<n>` (default: 3)
- Si supera el límite → "Ya tenés el máximo de homes (<n>)"
- `/homes` muestra: nombre, mundo, coordenadas
- `/delhome` elimina y confirma

**Mensajes:**
```
§aTeletransportado a home '<nombre>'.
§aHome '<nombre>' establecido.
§aHome '<nombre>' eliminado.
§6Tus homes:
  §f- §e<nombre> §7(<mundo> §f<x> <y> <z>§7)
§cHome '<nombre>' no encontrado.
§cYa tenés el máximo de homes (<n>).
§cNo tenés permiso para esto.
```

---

### /tpa — Teletransportación

**Sistema de invitaciones para teletransporte entre jugadores.** Requiere confirmación.

| Comando | Descripción | Permiso |
|---|---|---|
| `/tpa <jugador>` | Solicitar teleportarte A ese jugador | `anvil.tpa` |
| `/tpahere <jugador>` | Solicitar que el jugador venga A TI | `anvil.tpahere` |
| `/tpa accept` | Aceptar la solicitud pendiente | — |
| `/tpa deny` | Rechazar la solicitud pendiente | — |

**Comportamiento:**
- `/tpa Steve` → Steve recibe: "Colsson quiere teleportarse a ti. /tpa accept o /tpa deny"
- `/tpahere Steve` → Steve recibe: "Colsson quiere que vengas a su ubicación. /tpa accept o /tpa deny"
- Timeout: 60 segundos (configurable). Si no se responde → solicitud expira
- Solo 1 solicitud pendiente por jugador
- Si el otro jugador está offline → "Jugador no encontrado"
- Si se usa `/tpa accept` sin solicitud → "No tenés solicitud pendiente"
- `anvil.tpa.admin` → ejecuta el TPA sin necesidad de confirmación

**Mensajes:**
```
§aSolicitud enviada a <jugador>. Esperando respuesta...
§a<jugador> quiere teleportarse a ti. §e/tpa accept §7o §c/tpa deny
§a<jugador> quiere que vengas a su ubicación. §e/tpa accept §7o §c/tpa deny
§aTeletransportado a <jugador>.
§a<jugador> ha sido teletransportado a ti.
§cSolicitud rechazada.
§cNo tenés solicitud pendiente.
§cLa solicitud ha expirado.
§cJugador no encontrado.
```

---

### /spawn — Punto de spawn

**Teletransportarse al punto de spawn del servidor.**

| Comando | Descripción | Permiso |
|---|---|---|
| `/spawn` | Ir al spawn | `anvil.spawn` |
| `/spawn set` | Establecer spawn en tu ubicación | `anvil.spawn.admin` |

**Comportamiento:**
- `/spawn` → teletransporta al spawn (guardado en config o DB)
- `/spawn set` → guarda la ubicación actual como spawn
- El spawn se guarda en la DB de Anvil (tabla `anvil_spawn`)
- Si no hay spawn configurado → "Spawn no configurado"

**Mensajes:**
```
§aTeletransportado al spawn.
§aSpawn establecido en tu ubicación.
§cSpawn no configurado. Un admin debe usar /spawn set.
§cNo tenés permiso para esto.
```

---

### /kick — Expulsar

**Expulsar un jugador del servidor.**

| Comando | Descripción | Permiso |
|---|---|---|
| `/kick <jugador>` | Expulsar sin razón | `anvil.kick` |
| `/kick <jugador> <razón>` | Expulsar con razón | `anvil.kick` |

**Comportamiento:**
- Expulsa al jugador del servidor
- El jugador expulsado ve el mensaje de razón
- Se registra en el audit log de Beacon
- No se puede expulsar a un jugador con `anvil.kick.exempt`
- No se puede expulsar a uno mismo

**Mensajes:**
```
§a<jugador> ha sido expulsado.
§c<jugador> es inmune al kick.
§cNo podés expulsarte a vos mismo.
§cNo tenés permiso para esto.
```

---

### /ban — Banear

**Banear un jugador del servidor (temporal o permanente).**

| Comando | Descripción | Permiso |
|---|---|---|
| `/ban <jugador>` | Banear permanentemente | `anvil.ban` |
| `/ban <jugador> <razón>` | Ban con razón | `anvil.ban` |
| `/ban <jugador> <días>` | Ban temporal (días) | `anvil.ban.temp` |
| `/ban <jugador> <días> <razón>` | Ban temporal con razón | `anvil.ban.temp` |
| `/unban <jugador>` | Desbanear jugador | `anvil.ban` |

**Comportamiento:**
- Ban permanente: el jugador no puede volver a entrar
- Ban temporal: se calcula la fecha de expiración
- Al intentar entrar → "Estás baneado. Razón: <razón>. Expira: <fecha>"
- `/unban` remueve el ban
- No se puede banear a un jugador con `anvil.ban.exempt`
- Se registra en audit log

**Mensajes:**
```
§a<jugador> ha sido baneado.
§a<jugador> ha sido baneado por <días> días.
§a<jugador> ha sido desbaneado.
§c<jugador> es inmune al ban.
§cNo podés banear a tu mismo.
§cNo tenés permiso para esto.
```

---

### /mute — Silenciar

**Silenciar un jugador (no puede escribir en chat).**

| Comando | Descripción | Permiso |
|---|---|---|
| `/mute <jugador>` | Silenciar | `anvil.mute` |
| `/mute <jugador> <razón>` | Silenciar con razón | `anvil.mute` |
| `/unmute <jugador>` | Quitar silencio | `anvil.mute` |

**Comportamiento:**
- El jugador silenciado no puede escribir en el chat
- Si intenta escribir → "Estás silenciado. Razón: <razón>"
- `/unmute` remueve el silencio
- No se puede silenciar a un jugador con `anvil.mute.exempt`
- Se registra en audit log

**Mensajes:**
```
§a<jugador> ha sido silenciado.
§a<jugador> ha sido des-silenciado.
§c<jugador> es inmune al mute.
§cNo podés silenciarte a vos mismo.
§cNo tenés permiso para esto.
§cEstás silenciado. Razón: <razón>
```

---

### /gamemode — Modo de juego

**Cambiar el modo de juego (survival, creative, adventure, spectator).**

| Comando | Descripción | Permiso |
|---|---|---|
| `/gamemode <modo>` | Cambiar tu modo | `anvil.gamemode` |
| `/gamemode <modo> <jugador>` | Cambiar modo de otro | `anvil.gamemode.other` |

**Modos válidos:** `survival`/`s`/`0`, `creative`/`c`/`1`, `adventure`/`a`/`2`, `spectator`/`sp`/`3`

**Comportamiento:**
- Acepta nombre completo o abreviatura
- Si el modo es inválido → "Modo inválido: <modo>"
- Notifica al jugador si se cambia el de otro

**Mensajes:**
```
§aModo de juego cambiado a <modo>.
§aModo de juego de <jugador> cambiado a <modo>.
§cModo inválido: <modo>. Usa: survival, creative, adventure, spectator.
§cNo tenés permiso para esto.
```

---

### /heal — Curar

**Restablecer vida y saturación del jugador.**

| Comando | Descripción | Permiso |
|---|---|---|
| `/heal` | Curarse a uno mismo | `anvil.heal` |
| `/heal <jugador>` | Curar a otro jugador | `anvil.heal.other` |

**Comportamiento:**
- Restablece vida a 20 (máximo)
- Restablece saturación a 20
- Restablece puntos de hambre a 20
- Notifica al jugador si se cura a otro

**Mensajes:**
```
§aVida restablecida.
§a<jugador> ha sido curado.
§cNo tenés permiso para esto.
```

---

### /feed — Saciar

**Restablecer hambre del jugador.**

| Comando | Descripción | Permiso |
|---|---|---|
| `/feed` | Saciarte a uno mismo | `anvil.feed` |
| `/feed <jugador>` | Saciar a otro jugador | `anvil.feed.other` |

**Comportamiento:**
- Restablece puntos de hambre a 20
- Restablece saturación a 20
- Notifica al jugador si se sacia a otro

**Mensajes:**
```
§aHambre restablecida.
§aHambre de <jugador> restablecida.
§cNo tenés permiso para esto.
```

---

### /speed — Velocidad

**Cambiar la velocidad de movimiento.**

| Comando | Descripción | Permiso |
|---|---|---|
| `/speed <velocidad>` | Cambiar tu velocidad | `anvil.speed` |
| `/speed <velocidad> <jugador>` | Cambiar velocidad de otro | `anvil.speed` (con `.other`) |

**Valores:** 1-10 (1 = normal, 10 = máximo)

**Comportamiento:**
- Velocidad 1 = velocidad normal
- Velocidad 10 = velocidad máxima
- Si está volando, afecta la velocidad de vuelo
- Si el valor es inválido → "Velocidad inválida. Usa 1-10."

**Mensajes:**
```
§aVelocidad cambiada a <velocidad>.
§aVelocidad de <jugador> cambiada a <velocidad>.
§cVelocidad inválida. Usa 1-10.
§cNo tenés permiso para esto.
```

---

### /vanish — Invisibilidad

**Volverse invisible.** Los mobs no te atacan, los jugadores no te ven.

| Comando | Descripción | Permiso |
|---|---|---|
| `/vanish` | Activar/desactivar vanish propio | `anvil.vanish` |
| `/vanish <jugador>` | Activar/desactivar vanish de otro | `anvil.vanish.other` |

**Comportamiento:**
- Si NO estás en vanish → te activas, nadie te ve
- Si SÍ estás en vanish → te desactivas, vuelves a ser visible
- Mobs no te detectan (no te atacan)
- Jugadores no te ven en la lista TAB
- Jugadores no te ven sobre la cabeza
- Al desaparecer, los otros jugadores no reciben notificación
- El vanish persiste hasta que se desactivi manually

**Mensajes:**
```
§aVanish activado. Nadie te ve.
§cVanish desactivado. Ya eres visible.
§aVanish activado para <jugador>.
§cVanish desactivado para <jugador>.
§cNo tenés permiso para esto.
```

---

### /anvil — Help

**Muestra la ayuda del plugin.**

| Comando | Descripción |
|---|---|
| `/anvil` | Mostrar ayuda |
| `/anvil help` | Mostrar ayuda |
| `/anvil info` | Información del plugin |

---

## Modelo de Dominio

```java
// Hogar del jugador
record Home(
    UUID ownerUuid,
    String name,
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
)

// Solicitud de teleportación
record TeleportRequest(
    UUID requesterUuid,
    UUID targetUuid,
    TeleportType type,      // TPA o TPA_HERE
    long createdAtMillis,   // System.currentTimeMillis()
    boolean accepted        // true si fue aceptada
)

// Tipo de teleportación
enum TeleportType {
    TPA,          // /tpa → voy a la ubicación del otro
    TPA_HERE      // /tpahere → el otro viene a mi ubicación
}

// Estado de vanish
enum VanishState {
    ACTIVE,       // Jugador es invisible
    INACTIVE      // Jugador es visible
}
```

---

## Persistencia

### Tablas MySQL

Solo Anvil crea tablas propias. Permisos y grupos viven en Beacon.

```sql
-- Homes de jugadores
CREATE TABLE anvil_homes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_uuid CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL DEFAULT 'default',
    world VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw FLOAT NOT NULL DEFAULT 0,
    pitch FLOAT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(owner_uuid, name)
);

-- Spawn del servidor
CREATE TABLE anvil_spawn (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    world VARCHAR(64) NOT NULL,
    x DOUBLE NOT NULL,
    y DOUBLE NOT NULL,
    z DOUBLE NOT NULL,
    yaw FLOAT NOT NULL DEFAULT 0,
    pitch FLOAT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Bans
CREATE TABLE anvil_bans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    banned_by CHAR(36) DEFAULT NULL,
    expires_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(player_uuid)
);

-- Mutes
CREATE TABLE anvil_mutes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_uuid CHAR(36) NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    muted_by CHAR(36) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(player_uuid)
);
```

---

## Configuración

```yaml
# plugins/Anvil/config.yml

# Beacon (conexión a la DB de permisos)
beacon:
  auto-connect: true    # Detectar Beacon automáticamente

# TPA
tpa:
  timeout-seconds: 60   # Tiempo máximo para aceptar (segundos)
  require-confirmation: true

# Homes
homes:
  max-default: 3        # Límite default si no hay permiso anvil.home.max.<n>

# Spawn
spawn:
  world: lobby          # Mundo default del spawn
  x: 0.5
  y: 65
  z: 0.5

# Mensajes
messages:
  prefix: "§6[Anvil] §r"
  fly-enabled: "§aVuelo activado."
  fly-disabled: "§cVuelo desactivado."
  home-set: "§aHome '<nombre>' establecido."
  home-teleport: "§aTeletransportado a home '<nombre>'."
  home-deleted: "§aHome '<nombre>' eliminado."
  home-not-found: "§cHome '<nombre>' no encontrado."
  home-limit: "§cYa tenés el máximo de homes (<n>)."
  tpa-sent: "§aSolicitud enviada a <jugador>."
  tpa-received: "§a<jugador> quiere teleportarse a ti."
  tpa-here-received: "§a<jugador> quiere que vengas a su ubicación."
  tpa-accepted: "§aTeletransportado a <jugador>."
  tpa-denied: "§cSolicitud rechazada."
  tpa-expired: "§cLa solicitud ha expirado."
  tpa-none: "§cNo tenés solicitud pendiente."
  spawn-set: "§aSpawn establecido."
  spawn-teleport: "§aTeletransportado al spawn."
  spawn-not-set: "§cSpawn no configurado."
  kicked: "§a<jugador> ha sido expulsado."
  banned: "§a<jugador> ha sido baneado."
  unbanned: "§a<jugador> ha sido desbaneado."
  muted: "§a<jugador> ha sido silenciado."
  unmuted: "§a<jugador> ha sido des-silenciado."
  gamemode-changed: "§aModo de juego cambiado a <modo>."
  heal-done: "§aVida restablecida."
  feed-done: "§aHambre restablecida."
  speed-changed: "§aVelocidad cambiada a <velocidad>."
  vanish-enabled: "§aVanish activado. Nadie te ve."
  vanish-disabled: "§cVanish desactivado. Ya eres visible."
  no-permission: "§cNo tenés permiso para esto."
  player-not-found: "§cJugador no encontrado."
  cannot-self: "§cNo podés hacer esto en vos mismo."
  immune: "§c<jugador> es inmune a esto."
```

---

## Arquitectura

```
Comando (Paper) → AnvilLogic (testable) → BeaconAPI (permisos) → Paper API (acción)
       ↓                    ↓
  PaperCommandExecutor   HomeRepository (MySQL)
       ↓                    ↓
  TabCompletionEngine   CacheManager
```

### Flujo de /fly

```
1. Jugador ejecuta /fly
2. PaperCommandExecutor → AnvilCommandRouter
3. FlyHandler.execute()
   a. Verificar permiso: api.hasPermission(uuid, "anvil.fly", world)
   b. Si tiene permiso:
      - Toggle fly: player.setAllowFlight(!player.getAllowFlight())
      - Enviar mensaje de confirmación
   c. Si no tiene permiso:
      - Enviar mensaje de error
4. Listo
```

### Flujo de /home

```
1. Jugador ejecuta /home lobby
2. PaperCommandExecutor → AnvilCommandHandler
3. HomeHandler.execute()
   a. Verificar permiso: api.hasPermission(uuid, "anvil.home", world)
   b. Buscar home en DB: homeRepo.find(uuid, "lobby")
   c. Si existe:
      - Obtener ubicación del home
      - player.teleport(location)
      - Enviar mensaje de confirmación
   d. Si no existe:
      - Enviar mensaje de error
4. Listo
```

---

## Fases de Desarrollo

### Fase 0 — Especificación

**Tag:** `v0.0.0-fase0`

**Entregables:**
- [x] Este archivo (INDICATIVO.md) con todo detallado
- [x] SPEC.md del plugin

---

### Fase 1 — Modelo de Dominio

**Tag:** `v0.1.0-fase1`

**Objetivo:** Crear todas las clases de dominio (sin dependencias externas).

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/model/
  ├── Home.java              record(UUID, String, String, double, double, double, float, float)
  ├── TeleportRequest.java   record(UUID, UUID, TeleportType, long, boolean)
  ├── TeleportType.java      enum(TPA, TPA_HERE)
  ├── VanishState.java       enum(ACTIVE, INACTIVE)
  └── Gamemode.java          enum(SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR)
```

**Tests:**
```
src/test/java/com/colsson/anvil/model/
  ├── HomeTest.java
  ├── TeleportRequestTest.java
  ├── GamemodeTest.java
  └── VanishStateTest.java
```

**Criterios:**
- Todos los records son inmutables
- constructores completos
- equals/hashCode/toString funcionan
- Tests pasan al 100%

---

### Fase 2 — Lógica de Negocio

**Tag:** `v0.2.0-fase2`

**Objetivo:** Implementar la lógica central (testable sin DB ni Paper).

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/logic/
  ├── FlyManager.java        toggle fly, verificar permiso
  ├── HomeManager.java       CRUD homes, verificar límites
  ├── TeleportManager.java   TPA/TPAHere, timeout, aceptar/rechazar
  ├── SpawnManager.java      obtener/establecer spawn
  ├── BanManager.java        ban/unban, temporal/permanente
  ├── MuteManager.java       mute/unmute
  ├── GamemodeManager.java   cambiar gamemode
  ├── HealManager.java       curar jugador
  ├── FeedManager.java       saciar jugador
  ├── SpeedManager.java      cambiar velocidad
  └── VanishManager.java     activar/desactivar vanish
```

**Tests:**
```
src/test/java/com/colsson/anvil/logic/
  ├── FlyManagerTest.java
  ├── HomeManagerTest.java
  ├── TeleportManagerTest.java
  ├── SpawnManagerTest.java
  ├── BanManagerTest.java
  ├── MuteManagerTest.java
  ├── GamemodeManagerTest.java
  ├── HealManagerTest.java
  ├── FeedManagerTest.java
  ├── SpeedManagerTest.java
  └── VanishManagerTest.java
```

**Criterios:**
- Cada manager es una clase con métodos testables
- No depende de Paper (usa interfaces/records)
- Tests exhaustivos con escenarios variados

---

### Fase 3 — Persistencia

**Tag:** `v0.3.0-fase3`

**Objetivo:** Repositorios MySQL + tests con SQLite.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/persistence/
  ├── DatabaseManager.java       HikariCP connection pool
  ├── MigrationManager.java      ejecutar DDL automático
  ├── HomeRepository.java        CRUD homes
  ├── SpawnRepository.java       obtener/establecer spawn
  ├── BanRepository.java         ban/unban
  └── MuteRepository.java        mute/unmute
```

**Tests:**
```
src/test/java/com/colsson/anvil/persistence/
  ├── TestDatabaseHelper.java    SQLite in-memory
  ├── HomeRepositoryTest.java
  ├── SpawnRepositoryTest.java
  ├── BanRepositoryTest.java
  └── MuteRepositoryTest.java
```

**Reglas:**
- Pool size 1 para SQLite (evita deadlocks)
- Todas las queries usan el mismo Connection
- MySQL credentials: localhost:3306, plugins/root, db: anvil

---

### Fase 4 — Caché

**Tag:** `v0.4.0-fase4`

**Objetivo:** Cache en memoria para homes y datos frecuentes.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/cache/
  └── CacheManager.java          ConcurrentHashMap + TTL
```

**Tests:**
```
src/test/java/com/colsson/anvil/cache/
  └── CacheManagerTest.java
```

**Keys:**
- `home:{uuid}:{name}` → Home
- `spawn` → Location
- `ban:{uuid}` → BanRecord
- `mute:{uuid}` → MuteRecord

---

### Fase 5 — API Pública

**Tag:** `v0.5.0-fase5`

**Objetivo:** Interfaz pública para que otros plugins consuman Anvil.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/api/
  ├── AnvilAPI.java          interfaz
  └── AnvilAPIImpl.java      implementación
```

**Métodos:**
```java
// Homes
List<Home> getHomes(UUID player);
Optional<Home> getHome(UUID player, String name);
void setHome(UUID player, String name, Location loc);
void deleteHome(UUID player, String name);

// TPA
void sendTpa(UUID from, UUID target);
void sendTpaHere(UUID from, UUID target);
void acceptTpa(UUID player);
void denyTpa(UUID player);

// Spawn
Optional<Location> getSpawn();
void setSpawn(Location loc);

// Moderación
void ban(UUID player, String reason, long durationMillis);
void unban(UUID player);
void mute(UUID player, String reason);
void unmute(UUID player);
boolean isBanned(UUID player);
boolean isMuted(UUID player);

// Utilidades
void toggleFly(Player player);
void heal(Player player);
void feed(Player player);
void setSpeed(Player player, int speed);
void toggleVanish(Player player);
```

---

### Fase 6 — Comandos

**Tag:** `v0.6.0-fase6`

**Objetivo:** Sistema de comandos Paper-independent.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/commands/
  ├── AnvilCommandRouter.java
  ├── CommandHandler.java         interfaz
  ├── CommandContext.java          abstracción de CommandSender
  ├── MessageHelper.java          formatos de respuesta
  └── handlers/
      ├── FlyCommandHandler.java
      ├── HomeCommandHandler.java
      ├── SetHomeCommandHandler.java
      ├── DelHomeCommandHandler.java
      ├── HomesCommandHandler.java
      ├── TpaCommandHandler.java
      ├── TpahereCommandHandler.java
      ├── TpaAcceptCommandHandler.java
      ├── TpaDenyCommandHandler.java
      ├── SpawnCommandHandler.java
      ├── SpawnSetCommandHandler.java
      ├── KickCommandHandler.java
      ├── BanCommandHandler.java
      ├── UnbanCommandHandler.java
      ├── MuteCommandHandler.java
      ├── UnmuteCommandHandler.java
      ├── GamemodeCommandHandler.java
      ├── HealCommandHandler.java
      ├── FeedCommandHandler.java
      ├── SpeedCommandHandler.java
      ├── VanishCommandHandler.java
      ├── AnvilHelpCommandHandler.java
      └── AnvilInfoCommandHandler.java
```

**Tests:**
```
src/test/java/com/colsson/anvil/commands/
  ├── AnvilCommandRouterTest.java
  └── handlers/
      ├── FlyCommandHandlerTest.java
      ├── HomeCommandHandlerTest.java
      └── ... (cada handler)
```

---

### Fase 7 — Configuración

**Tag:** `v0.7.0-fase7`

**Objetivo:** Config + POJO central.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/
  ├── AnvilPlugin.java       POJO central (sin Paper)
  └── config/
      └── AnvilConfig.java   POJO con defaults

src/main/resources/
  └── config.yml
```

**Tests:**
```
src/test/java/com/colsson/anvil/config/
  └── AnvilConfigTest.java
```

---

### Fase 8 — Integración Paper API

**Tag:** `v0.8.0-fase8`

**Objetivo:** Adaptador Paper + JAR deployable.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/
  ├── AnvilJavaPlugin.java       extends JavaPlugin
  ├── PaperCommandExecutor.java  CommandSender → CommandContext
  └── listener/
      ├── PlayerJoinListener.java
      ├── PlayerQuitListener.java
      └── PlayerChatListener.java  (para mute check)

src/main/resources/
  ├── plugin.yml
  └── config.yml
```

**Validación:**
1. `./gradlew test` — todos pasan
2. `./gradlew shadowJar` — JAR generado
3. Copiar a `plugins/` del servidor
4. Reiniciar servidor
5. Verificar: plugin carga, comandos funcionan

---

### Fase 9 — Tab Completion

**Tag:** `v0.9.0-tab-completion`

**Objetivo:** Autocomplete completo para todos los comandos.

**Archivos a crear:**
```
src/main/java/com/colsson/anvil/commands/
  └── TabCompletionEngine.java    lógica pura (Supplier-based)
```

**Tests:**
```
src/test/java/com/colsson/anvil/commands/
  └── TabCompletionLogicTest.java
```

**Placeholders:**
| Placeholder | Uso |
|---|---|
| `<jugador>` | Nombre de jugador online |
| `<nombre>` | Nombre de home/entidad |
| `<modo>` | survival, creative, adventure, spectator |
| `<velocidad>` | 1-10 |
| `<razón>` | Razón opcional |

---

### Fase 10 — Documentación

**Tag:** `v1.0.0`

**Objetivo:** README.md completo.

**Entregables:**
- Descripción
- Instalación
- Comandos (tabla completa)
- API (ejemplos)
- Configuración
- Arquitectura

---

## Flujo resumen

```
Fase 0  [✓] Especificación (este archivo) → v0.0.0-fase0
Fase 1  [ ] Modelo de dominio              → v0.1.0-fase1
Fase 2  [ ] Lógica de negocio              → v0.2.0-fase2
Fase 3  [ ] Persistencia                   → v0.3.0-fase3
Fase 4  [ ] Caché                          → v0.4.0-fase4
Fase 5  [ ] API pública                    → v0.5.0-fase5
Fase 6  [ ] Comandos                       → v0.6.0-fase6
Fase 7  [ ] Configuración                  → v0.7.0-fase7
Fase 8  [ ] Integración Paper              → v0.8.0-fase8
Fase 9  [ ] Tab completion                 → v0.9.0-tab-completion
Fase 10 [ ] Documentación                  → v1.0.0
```

---

## Reglas de oro

1. **Nunca avanzar de fase sin tests pasando al 100%**
2. **SPEC.md es la fuente de verdad** — si hay conflicto, el SPEC gana
3. **Cada fase es un commit + tag** — no mezclar fases
4. **Testear en servidor solo en Fase 8+**
5. **Commit messages en español**
6. **BeaconAPI para permisos** — nunca acceder a DB de Beacon directamente
7. **Shadow JAR obligatorio** — sombreado para Paper
8. **World-aware** — todos los permisos se verifican con contexto de mundo
