package com.colsson.beacon.resolver;

import com.colsson.beacon.model.*;

import java.util.*;

/**
 * Motor de resolución de permisos de Beacon.
 *
 * <p>Funciona independientemente de Paper y MySQL.
 * Solo depende del modelo de dominio.
 *
 * <p>Orden de resolución:
 * <ol>
 *   <li>Permisos directos del usuario (máxima prioridad)</li>
 *   <li>Grupos del usuario, ordenados por prioridad descendente</li>
 *   <li>Ancestros de cada grupo (misma prioridad que el grupo hijo)</li>
 *   <li>Conflicto TRUE vs FALSE: gana mayor prioridad numérica</li>
 *   <li>UNDEFINED nunca compite como negación</li>
 * </ol>
 */
public class PermissionResolver {

    /**
     * Resuelve el estado de un permiso para un usuario.
     *
     * @param user       el usuario a consultar
     * @param permission el nodo de permiso (ej: "anvil.fly")
     * @return PermissionResult con estado, fuente y traza completa
     */
    public PermissionResult resolve(User user, String permission) {
        if (user == null || permission == null || permission.isBlank()) {
            return PermissionResult.undefined();
        }

        // 1. Permisos directos del usuario — máxima prioridad
        PermissionState directState = user.getDirectPermissionState(permission);
        if (directState.isDefined()) {
            return new PermissionResult(
                directState,
                "direct",
                "Direct user permission: " + permission + " = " + directState
            );
        }

        // 2. Recopilar permisos definidos de todos los grupos y ancestros
        List<SourceEntry> defined = collectDefinedPermissions(user, permission);

        // 3. Si no hay ninguno definido → UNDEFINED
        if (defined.isEmpty()) {
            return PermissionResult.undefined();
        }

        // 4. Seleccionar el de mayor prioridad
        SourceEntry winner = selectByPriority(defined);

        return new PermissionResult(
            winner.state(),
            winner.sourceName(),
            buildTrace(permission, winner, defined)
        );
    }

    /**
     * Recopila todos los permisos definidos (TRUE o FALSE) para un nodo
     * dado, provenientes de los grupos directos del usuario y sus ancestros.
     */
    private List<SourceEntry> collectDefinedPermissions(User user, String permission) {
        List<SourceEntry> entries = new ArrayList<>();

        for (Group group : user.groups()) {
            collectFromGroupAndAncestors(group, permission, entries);
        }

        return entries;
    }

    private void collectFromGroupAndAncestors(Group group, String permission,
                                               List<SourceEntry> entries) {
        // Permiso del grupo actual
        addIfDefined(group, permission, entries);

        // Permisos de ancestros (misma prioridad que el grupo hijo)
        for (Group ancestor : group.ancestors()) {
            addIfDefined(ancestor, permission, entries);
        }
    }

    private void addIfDefined(Group group, String permission, List<SourceEntry> entries) {
        PermissionState state = group.getPermissionState(permission);
        if (state.isDefined()) {
            entries.add(new SourceEntry(state, group.priority(), group.name()));
        }
    }

    /**
     * De entre las entradas definidas, selecciona la de mayor prioridad numérica.
     * Si hay TRUE y FALSE con la misma prioridad, el orden de inserción desempata
     * (grupos directos antes que ancestros).
     */
    private SourceEntry selectByPriority(List<SourceEntry> entries) {
        SourceEntry winner = entries.getFirst();
        for (int i = 1; i < entries.size(); i++) {
            SourceEntry current = entries.get(i);
            if (current.priority() > winner.priority()) {
                winner = current;
            }
        }
        return winner;
    }

    private String buildTrace(String permission, SourceEntry winner,
                               List<SourceEntry> all) {
        StringBuilder sb = new StringBuilder();
        sb.append("Permission: ").append(permission).append("\n");

        sb.append("Defined sources:\n");
        for (SourceEntry entry : all) {
            sb.append("  - ").append(entry.sourceName())
              .append(" (priority ").append(entry.priority()).append("): ")
              .append(entry.state()).append("\n");
        }

        sb.append("Winner: ").append(winner.sourceName())
          .append(" → ").append(winner.state());

        return sb.toString();
    }

    /**
     * Entrada interna que representa una fuente de permiso definida.
     */
    record SourceEntry(PermissionState state, int priority, String sourceName) {}
}
