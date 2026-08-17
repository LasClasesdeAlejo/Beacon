# Beacon

Sistema de permisos, grupos, herencia y auditoría para Minecraft Paper.

## Requisitos

- Paper 26.2+ (Minecraft 1.21+)
- Java 21+
- MySQL 8.0+

## Instalación

1. Descargá `Beacon-0.8.0.jar` desde la carpeta `build/libs/`
2. Copialo a la carpeta `plugins/` de tu servidor Paper
3. Iniciá el servidor — Beacon crea las tablas automáticamente
4. Verificá en la consola que aparezca:
   ```
   [Beacon] Beacon habilitado — servidor: default
   ```

## Configuración

El archivo `plugins/Beacon/config.yml` se genera automáticamente:

```yaml
mysql:
  host: localhost
  port: 3306
  database: beacon
  username: root
  password: ""

cache:
  enabled: true
  ttl-seconds: 300
  max-size: 1000

server-name: default
auto-migrate: true
```

**IMPORTANTE:** Creá la base de datos `beacon` en MySQL antes de iniciar:
```sql
CREATE DATABASE beacon;
CREATE USER 'plugins'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON beacon.* TO 'plugins'@'localhost';
FLUSH PRIVILEGES;
```

## Comandos

### Generales

| Comando | Descripción |
|---|---|
| `/beacon` | Muestra ayuda |
| `/beacon help` | Muestra ayuda |
| `/beacon info` | Información del plugin |
| `/beacon reload` | Recarga la caché |

### Usuarios — Consultas

| Comando | Descripción |
|---|---|
| `/beacon user <jugador> info` | Info completa del jugador |
| `/beacon user <jugador> groups` | Grupos asignados |
| `/beacon user <jugador> permissions` | Permisos directos |

### Usuarios — Modificaciones

| Comando | Descripción |
|---|---|
| `/beacon user <jugador> group add <grupo> [razón]` | Asigna grupo |
| `/beacon user <jugador> group remove <grupo> [razón]` | Remueve grupo |
| `/beacon user <jugador> permission set <permiso> <true\|false> [razón]` | Asigna permiso |
| `/beacon user <jugador> permission remove <permiso> [razón]` | Remueve permiso |
| `/beacon user <jugador> permission clear [razón]` | Limpia permisos directos |

### Grupos — Gestión

| Comando | Descripción |
|---|---|
| `/beacon group list` | Lista todos los grupos |
| `/beacon group create <nombre> [razón]` | Crea un grupo |
| `/beacon group <nombre> info` | Info del grupo |
| `/beacon group <nombre> delete [razón]` | Elimina el grupo |

### Grupos — Edición

| Comando | Descripción |
|---|---|
| `/beacon group <nombre> edit name <nuevo> [razón]` | Renombra el grupo |
| `/beacon group <nombre> edit priority <numero> [razón]` | Cambia prioridad |
| `/beacon group <nombre> edit description <texto> [razón]` | Cambia descripción |

### Grupos — Herencia

| Comando | Descripción |
|---|---|
| `/beacon group <nombre> parents` | Lista padres del grupo |
| `/beacon group <nombre> parent set <padre> [razón]` | Establece herencia |
| `/beacon group <nombre> parent remove <padre> [razón]` | Remueve herencia |

### Grupos — Permisos

| Comando | Descripción |
|---|---|
| `/beacon group <nombre> permission set <permiso> <true\|false> [razón]` | Asigna permiso |
| `/beacon group <nombre> permission remove <permiso> [razón]` | Remueve permiso |
| `/beacon group <nombre> permission clear [razón]` | Limpia permisos |

### Permisos Globales

| Comando | Descripción |
|---|---|
| `/beacon permission list` | Lista permisos |
| `/beacon permission info <permiso>` | Info de un permiso |
| `/beacon permission search <texto>` | Busca permisos |

### Diagnóstico

| Comando | Descripción |
|---|---|
| `/beacon check <jugador> <permiso>` | Verifica si tiene un permiso |
| `/beacon groups tree` | Muestra árbol de herencia |
| `/beacon debug` | Debug general |
| `/beacon debug user <jugador>` | Debug de usuario |
| `/beacon debug permission <jugador> <permiso>` | Debug de permiso |

### Historial

| Comando | Descripción |
|---|---|
| `/beacon history` | Últimas acciones |
| `/beacon history user <jugador>` | Historial de usuario |
| `/beacon history group <grupo>` | Historial de grupo |
| `/beacon history permission <permiso>` | Historial de permiso |

## API Pública

Otros plugins (Anvil, Loom) consumen Beacon mediante `BeaconAPI`:

```java
BeaconAPI api = /* obtener instancia */;

// Consultas
Optional<User> user = api.getUser(uuid);
Optional<User> userByName = api.getUserByName("Colsson");
boolean hasPerm = api.hasPermission(uuid, "anvil.fly");
List<Group> groups = api.getGroups();

// Modificaciones
api.createGroup("vip", 10, "Grupo VIP", "admin", "Creación inicial");
api.addUserToGroup(uuid, "vip", "admin", "Rango VIP");
api.setUserPermission(uuid, "anvil.fly", true, "admin", null);
```

## Reglas de Negocio

- **Sin primaryGroup** — un usuario puede tener múltiples grupos
- **Sin `group set`** — solo `add`/`remove` para gestionar grupos
- **UNDEFINED ≠ FALSE** — un permiso no definido no bloquea
- **Excepciones directas** — tienen máxima prioridad sobre grupos
- **Razones opcionales** — solo en modificaciones, nunca en consultas
- **Auditoría** — toda modificación queda registrada en `audit_log`
- **Ciclos** — la herencia detecta y bloquea ciclos automáticamente

## Red

```
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

Todos los servidores apuntan al mismo MySQL. La cache es local por servidor.

## Licencia

Privada — Colsson Network
