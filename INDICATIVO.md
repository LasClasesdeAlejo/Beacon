# INDICATIVO — Proceso de Desarrollo de un Plugin Paper

> Template basado en el desarrollo de **Beacon** (permissions/groups plugin).
> Cada fase se completa, valida y taggea antes de avanzar a la siguiente.

---

## Convenciones

| Concepto | Regla |
|---|---|
| **SPEC.md** | Documento vivo, fuente de verdad. Todo código se valida contra él. |
| **Arquitectura** | `COMANDO → SERVICIO → DOMINIO → REPOSITORIO/CACHE → MYSQL` |
| **Tests** | Unitarios por fase. Cada fase incrementa el conteo. 0 failures para avanzar. |
| **Commits** | Mensajes en español. Un commit por fase completada. |
| **Tags** | `v{fase}.{incremental}-fase{N}` — ej: `v0.0.0-fase0`, `v0.1.0-fase1` |
| **Build** | Gradle, Java 21, JUnit 5, Shadow JAR |
| **Paquete** | `com.colsson.{nombre}` |

---

## Fase 0 — Especificación (SPEC.md)

**Objetivo:** Definir TODO el plugin antes de escribir una línea de código.

**Entregables:**
- `SPEC.md` con todas las secciones del plugin
- Arquitectura de capas definida
- Modelo de datos (tablas, relaciones)
- Comandos documentados (§11 equivalent)
- Reglas de negocio (resolver, herencia, etc.)

**Validación:** Revisión manual del SPEC completo.

**Tag:** `v0.0.0-fase0`

---

## Fase 1 — Modelo de Dominio

**Objetivo:** Crear todas las clases de dominio puras (sin dependencias externas).

**Entregables:**
- Clases de dominio inmutables (records o clases con campos finales)
- Constructores completos (todos los campos)
- Métodos de dominio (validación, igualdad, representación)
- Tests unitarios por cada clase

**Patrón:**
```
src/main/java/com/colsson/{plugin}/model/
  ├── EntidadPrincipal.java
  ├── SubEntidad.java
  ├── ValueObject.java
  └── ...
```

**Tests:** `src/test/java/com/colsson/{plugin}/model/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.1.0-fase1`

---

## Fase 2 — Lógica de Negocio (Resolver/Engine)

**Objetivo:** Implementar la lógica central del plugin, completamente testable sin DB ni Paper.

**Entregables:**
- Clase principal de lógica (ej: `PermissionResolver`, `ScoreEngine`, etc.)
- Entradas: objetos de dominio de la Fase 1
- Salidas: resultados de dominio
- Tests exhaustivos con escenarios variados

**Patrón:**
```
src/main/java/com/colsson/{plugin}/resolver/
  ├── {LogicEngine}.java
  └── ...
```

**Tests:** `src/test/java/com/colsson/{plugin}/resolver/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.2.0-fase2`

---

## Fase 3 — Persistencia (Repositories + Migrations)

**Objetivo:** Capa de acceso a datos con MySQL, migraciones, y tests con SQLite.

**Entregables:**
- `DatabaseManager` — conexión HikariCP
- `MigrationManager` — DDL automático
- Repositorios CRUD por entidad
- Tests con SQLite in-memory (pool size 1)

**Patrón:**
```
src/main/java/com/colsson/{plugin}/persistence/
  ├── DatabaseManager.java
  ├── MigrationManager.java
  ├── {Entity}Repository.java
  └── ...
```

**Reglas:**
- Pool size 1 para SQLite en tests (evita deadlocks)
- Todas las queries usan el mismo Connection dentro de una operación
- `groups` es reserved keyword en MySQL → usar backticks

**Tests:** `src/test/java/com/colsson/{plugin}/persistence/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.3.0-fase3`

---

## Fase 4 — Caché

**Objetivo:** Cache en memoria con TTL para reducir consultas a DB.

**Entregables:**
- `CacheManager` — ConcurrentHashMap + TTL
- Invalidación por clave, por prefijo, y total
- Configuración: TTL, max size, habilitado/deshabilitado
- Tests de TTL, invalidación, maxSize, disabled

**Patrón:**
```
src/main/java/com/colsson/{plugin}/cache/
  └── CacheManager.java
```

**Tests:** `src/test/java/com/colsson/{plugin}/cache/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.4.0-fase4`

---

## Fase 5 — API Pública

**Objetivo:** Interfaz pública para que otros plugins consuman la funcionalidad.

**Entregables:**
- `{Plugin}API` — interfaz (compileOnly, sin dependencia de Paper)
- `{Plugin}APIImpl` — implementación (usa repositories + cache)
- Flujo: `cache → MySQL → dominio`
- Tests de integración (API + repositories + cache)

**Patrón:**
```
src/main/java/com/colsson/{plugin}/api/
  ├── {Plugin}API.java       ← interfaz
  └── {Plugin}APIImpl.java   ← implementación
```

**Tests:** `src/test/java/com/colsson/{plugin}/api/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.5.0-fase5`

---

## Fase 6 — Comandos (Paper-Independent)

**Objetivo:** Sistema de comandos completo, testeable sin Paper.

**Entregables:**
- `CommandRouter` — parseo de args + dispatch
- `CommandHandler` — interfaz
- `CommandContext` — abstracción de CommandSender
- `MessageHelper` — formatos de respuesta
- Handlers por subcomando
- Tests de routing + cada handler

**Patrón:**
```
src/main/java/com/colsson/{plugin}/commands/
  ├── CommandRouter.java
  ├── CommandHandler.java
  ├── CommandContext.java
  ├── MessageHelper.java
  └── handlers/
      ├── {SubCommand}Handler.java
      └── ...
```

**Tests:** `src/test/java/com/colsson/{plugin}/commands/`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.6.0-fase6`

---

## Fase 7 — Configuración

**Objetivo:** Configuración del plugin (config.yml + POJO).

**Entregables:**
- `{Plugin}Config.java` — POJO con defaults
- `config.yml` — archivo de configuración
- `{Plugin}Plugin.java` — clase principal POJO (sin Paper)
  - Lifecycle: `enable()` / `disable()`
  - Acceso a repositories, cache, API
- Tests del POJO de configuración

**Patrón:**
```
src/main/java/com/colsson/{plugin}/
  ├── {Plugin}Plugin.java    ← POJO central
  └── config/
      └── {Plugin}Config.java
src/main/resources/
  └── config.yml
```

**Tests:** `src/test/java/com/colsson/{plugin}/config/` + `src/test/java/com/colsson/{plugin}/{Plugin}PluginTest.java`

**Validación:** `./gradlew test` — todos pasan.

**Tag:** `v0.7.0-fase7`

---

## Fase 8 — Integración Paper API

**Objetivo:** Adaptador final entre Paper y la lógica del plugin.

**Entregables:**
- `{Plugin}JavaPlugin extends JavaPlugin` — adapter
  - `onEnable()` / `onDisable()` → delega a POJO
  - Wiring de repositories → cache → API → router
- `PaperCommandExecutor` — CommandSender → CommandContext + TabCompleter
- Listeners (PlayerJoin, etc.) si aplica
- `plugin.yml` — `main:` apuntando al JavaPlugin
- Shadow JAR (HikariCP + MySQL Connector sombreados)
- Tests del adapter

**Patrón:**
```
src/main/java/com/colsson/{plugin}/
  ├── {Plugin}JavaPlugin.java     ← extends JavaPlugin
  ├── PaperCommandExecutor.java   ← CommandSender adapter
  └── listener/
      └── {Event}Listener.java
src/main/resources/
  ├── plugin.yml
  └── config.yml
```

**Tests:** `src/test/java/com/colsson/{plugin}/{Plugin}JavaPluginTest.java`

**Validación:**
1. `./gradlew test` — todos pasan
2. `./gradlew shadowJar` — JAR generado
3. Copiar a `plugins/` del servidor
4. Reiniciar servidor
5. Verificar: plugin carga, DB conecta, comandos funcionan

**Tag:** `v0.8.0-fase8`

---

## Fase 9 — Tab Completion Pulido

**Objetivo:** Autocomplete completo para todos los comandos del SPEC.

**Entregables:**
- `{Logic}Engine.java` — lógica pura sin Paper (Supplier-based)
- `{Plugin}JavaPlugin.java` — delega al engine
- Tests exhaustivos por cada ruta del SPEC

**Patrón:**
```
src/main/java/com/colsson/{plugin}/commands/
  └── TabCompletionEngine.java    ← lógica pura
src/main/java/com/colsson/{plugin}/
  └── PaperCommandExecutor.java   ← delega al engine
```

**Tests:** `src/test/java/com/colsson/{plugin}/commands/TabCompletionLogicTest.java`

**Placeholders estándar:**
| Placeholder | Uso |
|---|---|
| `<jugador>` | Nombre de jugador online |
| `<nombre>` | Nombre de entidad nueva |
| `<permiso>` | String de permiso |
| `<razón>` | Razón opcional de modificación |
| `<texto>` | Texto libre |

**Validación:**
1. `./gradlew test` — todos pasan
2. `./gradlew shadowJar` + deploy
3. Probar cada ruta en el juego

**Tag:** `v0.9.0-tab-completion`

---

## Fase 10 — Documentación (README.md)

**Objetivo:** Documentación completa del plugin.

**Entregables:**
- `README.md` con:
  - Descripción
  - Instalación (requisitos, JAR, config)
  - Comandos (tabla completa)
  - API (ejemplos de uso para otros plugins)
  - Diagrama de arquitectura (texto)

**Tag:** `v1.0.0` — Release candidate

---

## Flujo resumen

```
Fase 0  SPEC.md                    → v0.0.0-fase0
Fase 1  Modelo de dominio          → v0.1.0-fase1
Fase 2  Lógica de negocio          → v0.2.0-fase2
Fase 3  Persistencia               → v0.3.0-fase3
Fase 4  Caché                      → v0.4.0-fase4
Fase 5  API pública                → v0.5.0-fase5
Fase 6  Comandos                   → v0.6.0-fase6
Fase 7  Configuración              → v0.7.0-fase7
Fase 8  Integración Paper          → v0.8.0-fase8
Fase 9  Tab completion             → v0.9.0-tab-completion
Fase 10 Documentación              → v1.0.0
```

---

## Reglas de oro

1. **Nunca avanzar de fase sin tests pasando al 100%**
2. **SPEC.md es la fuente de verdad** — si hay conflicto, el SPEC gana
3. **Cada fase es un commit + tag** — no mezclar fases
4. **Testear en servidor solo cuando Paper lo requiera** (Fase 8+)
5. **Commit messages en español**
6. **No agregar dependencias sin verificar que el build las necesita**
7. **SQL con backticks para reserved keywords** (`groups`, `user`, `order`)
8. **Shadow JAR obligatorio** — HikariCP + MySQL Connector siempre sombreados
