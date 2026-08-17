package com.colsson.beacon.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Motor de tab completion puramente lógico, sin dependencias de Paper.
 *
 * <p>Cubre todas las rutas del SPEC §11:
 * <ul>
 *   <li>§11.1 Generales</li>
 *   <li>§11.2 Usuario — Consultas</li>
 *   <li>§11.3 Usuario — Modificaciones</li>
 *   <li>§11.4 Grupo — Gestión</li>
 *   <li>§11.5 Grupo — Edición</li>
 *   <li>§11.6 Grupo — Herencia</li>
 *   <li>§11.7 Grupo — Permisos</li>
 *   <li>§11.8 Permisos globales</li>
 *   <li>§11.9 Diagnóstico</li>
 *   <li>§11.10 Historial</li>
 * </ul>
 *
 * <p>Placeholders:
 * <code>&lt;jugador&gt;</code>, <code>&lt;grupo&gt;</code>,
 * <code>&lt;nombre&gt;</code>, <code>&lt;permiso&gt;</code>,
 * <code>&lt;razón&gt;</code>, <code>&lt;nuevoNombre&gt;</code>,
 * <code>&lt;numero&gt;</code>, <code>&lt;texto&gt;</code>,
 * <code>&lt;padre&gt;</code>, <code>&lt;mundo&gt;</code>
 */
public class TabCompletionEngine {

    private final Supplier<List<String>> playerNamesSupplier;
    private final Supplier<List<String>> groupNamesSupplier;

    public TabCompletionEngine(Supplier<List<String>> playerNamesSupplier,
                                Supplier<List<String>> groupNamesSupplier) {
        this.playerNamesSupplier = playerNamesSupplier;
        this.groupNamesSupplier = groupNamesSupplier;
    }

    /**
     * Calcula las completions para los args dados.
     *
     * @param args argumentos ya escritos (sin incluir "/beacon")
     * @return lista filtrada de completions posibles
     */
    public List<String> complete(String[] args) {
        int len = args.length;
        String a0 = len >= 1 ? args[0].toLowerCase() : "";
        String a1 = len >= 2 ? args[1].toLowerCase() : "";
        String a2 = len >= 3 ? args[2].toLowerCase() : "";
        String a3 = len >= 4 ? args[3].toLowerCase() : "";
        String last = args[len - 1].toLowerCase();

        List<String> c = new ArrayList<>();

        // ════════════════════════════════════════════════════
        // §11.1 — Nivel 1: comandos principales
        // ════════════════════════════════════════════════════
        if (len == 1) {
            c.addAll(List.of(
                "help", "info", "reload",
                "user", "group", "permission",
                "check", "groups", "debug", "history"
            ));
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.2 + §11.3 — /beacon user <jugador> ...
        // ════════════════════════════════════════════════════
        if ("user".equals(a0)) {
            switch (len) {
                // §11.2: user <TAB> → jugadores online
                case 2 -> c.addAll(playerNamesSupplier.get());

                // §11.2 + §11.3: user Colsson <TAB> → subcomandos
                case 3 -> c.addAll(List.of(
                    "info", "groups", "permissions",
                    "group", "permission"
                ));

                // §11.3: user Colsson group|permission <TAB> → acciones
                case 4 -> {
                    switch (a2) {
                        case "group" -> c.addAll(List.of("add", "remove"));
                        case "permission" -> c.addAll(List.of("set", "remove", "clear"));
                    }
                }

                // §11.3: user Colsson group add|remove <TAB> → grupo
                //         user Colsson permission set|remove <TAB> → <permiso>
                //         user Colsson permission clear <TAB> → <razón>
                case 5 -> {
                    switch (a2) {
                        case "group" -> c.addAll(groupNamesSupplier.get());
                        case "permission" -> {
                            if ("set".equals(a3) || "remove".equals(a3)) {
                                c.add("<permiso>");
                            } else if ("clear".equals(a3)) {
                                c.add("<razón>");
                            }
                        }
                    }
                }

                // §11.3: user Colsson group add vip <TAB> → <razón>
                //         user Colsson permission set test <TAB> → true|false
                //         user Colsson permission remove <TAB> → <mundo>|<razón>
                case 6 -> {
                    switch (a2) {
                        case "group" -> c.add("<razón>");
                        case "permission" -> {
                            if ("set".equals(a3)) c.addAll(List.of("true", "false"));
                            else if ("remove".equals(a3)) c.addAll(List.of("<mundo>", "<razón>"));
                        }
                    }
                }

                // §11.3: user Colsson permission set test true <TAB> → <mundo>|<razón>
                case 7 -> {
                    if ("permission".equals(a2) && "set".equals(a3)) {
                        c.addAll(List.of("<mundo>", "<razón>"));
                    }
                }

                // §11.3: user Colsson permission set test true world <TAB> → <razón>
                case 8 -> {
                    if ("permission".equals(a2) && "set".equals(a3)) {
                        c.add("<razón>");
                    }
                }
            }
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.4 + §11.5 + §11.6 + §11.7 — /beacon group ...
        // ════════════════════════════════════════════════════
        if ("group".equals(a0)) {
            switch (len) {
                // §11.4: group <TAB> → list, create
                case 2 -> c.addAll(List.of("list", "create"));

                // §11.4: group create <TAB> → <nombre>
                //         group admin <TAB> → subcomandos
                case 3 -> {
                    if ("create".equals(a1)) {
                        c.add("<nombre>");
                    } else {
                        c.addAll(List.of(
                            "info", "delete",
                            "edit", "parent", "permission", "parents"
                        ));
                    }
                }

                // §11.5: group admin edit <TAB> → name, priority, description
                //         §11.6: group admin parent <TAB> → set, remove
                //         §11.7: group admin permission <TAB> → set, remove, clear
                //         §11.4: group admin delete <TAB> → <razón>
                case 4 -> {
                    switch (a2) {
                        case "edit" -> c.addAll(List.of("name", "priority", "description"));
                        case "parent" -> c.addAll(List.of("set", "remove"));
                        case "permission" -> c.addAll(List.of("set", "remove", "clear"));
                        case "delete" -> c.add("<razón>");
                        case "info", "parents" -> { /* fin */ }
                    }
                }

                // §11.5: group admin edit name <TAB> → <nuevoNombre>
                //         §11.5: group admin edit priority <TAB> → <numero>
                //         §11.5: group admin edit description <TAB> → <texto>
                //         §11.6: group admin parent set|remove <TAB> → <padre>
                //         §11.7: group admin permission set|remove <TAB> → <permiso>
                //         §11.7: group admin permission clear <TAB> → <razón>
                case 5 -> {
                    switch (a2) {
                        case "edit" -> {
                            switch (a3) {
                                case "name" -> c.add("<nuevoNombre>");
                                case "priority" -> c.add("<numero>");
                                case "description" -> c.add("<texto>");
                            }
                        }
                        case "parent" -> c.addAll(groupNamesSupplier.get());
                        case "permission" -> {
                            if ("set".equals(a3) || "remove".equals(a3)) {
                                c.add("<permiso>");
                            } else if ("clear".equals(a3)) {
                                c.add("<razón>");
                            }
                        }
                    }
                }

                // §11.5: group admin edit name Nuevo <TAB> → <razón>
                //         §11.5: group admin edit priority 10 <TAB> → <razón>
                //         §11.5: group admin edit description Texto <TAB> → <razón>
                //         §11.6: group admin parent set vip <TAB> → <razón>
                //         §11.7: group admin permission set test <TAB> → true|false
                //         §11.7: group admin permission remove <TAB> → <mundo>|<razón>
                case 6 -> {
                    switch (a2) {
                        case "edit" -> c.add("<razón>");
                        case "parent" -> c.add("<razón>");
                        case "permission" -> {
                            if ("set".equals(a3)) c.addAll(List.of("true", "false"));
                            else if ("remove".equals(a3)) c.addAll(List.of("<mundo>", "<razón>"));
                        }
                    }
                }

                // §11.7: group admin permission set test true <TAB> → <mundo>|<razón>
                case 7 -> {
                    if ("permission".equals(a2) && "set".equals(a3)) {
                        c.addAll(List.of("<mundo>", "<razón>"));
                    }
                }

                // §11.7: group admin permission set test true world <TAB> → <razón>
                case 8 -> {
                    if ("permission".equals(a2) && "set".equals(a3)) {
                        c.add("<razón>");
                    }
                }
            }
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.8 — /beacon permission ...
        // ════════════════════════════════════════════════════
        if ("permission".equals(a0)) {
            switch (len) {
                case 2 -> c.addAll(List.of("list", "info", "search"));
                case 3 -> {
                    switch (a1) {
                        case "info" -> c.add("<permiso>");
                        case "search" -> c.add("<texto>");
                    }
                }
            }
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.9 — /beacon check ...
        // ════════════════════════════════════════════════════
        if ("check".equals(a0)) {
            switch (len) {
                case 2 -> c.addAll(playerNamesSupplier.get());
                case 3 -> c.add("<permiso>");
                case 4 -> c.add("<mundo>");
            }
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.9 — /beacon groups ...
        // ════════════════════════════════════════════════════
        if ("groups".equals(a0)) {
            if (len == 2) c.add("tree");
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.9 — /beacon debug ...
        // ════════════════════════════════════════════════════
        if ("debug".equals(a0)) {
            switch (len) {
                case 2 -> c.addAll(List.of("user", "permission"));
                case 3 -> {
                    if ("user".equals(a1) || "permission".equals(a1)) {
                        c.addAll(playerNamesSupplier.get());
                    }
                }
                case 4 -> {
                    if ("permission".equals(a1)) {
                        c.add("<permiso>");
                    }
                }
                case 5 -> {
                    if ("permission".equals(a1)) {
                        c.add("<mundo>");
                    }
                }
            }
            return filter(c, last);
        }

        // ════════════════════════════════════════════════════
        // §11.10 — /beacon history ...
        // ════════════════════════════════════════════════════
        if ("history".equals(a0)) {
            switch (len) {
                case 2 -> c.addAll(List.of("user", "group", "permission"));
                case 3 -> {
                    switch (a1) {
                        case "user" -> c.addAll(playerNamesSupplier.get());
                        case "group" -> c.addAll(groupNamesSupplier.get());
                        case "permission" -> c.add("<permiso>");
                    }
                }
            }
            return filter(c, last);
        }

        return filter(c, last);
    }

    // ── Helpers ────────────────────────────────────────────

    private List<String> filter(List<String> completions, String last) {
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(last))
            .toList();
    }
}
