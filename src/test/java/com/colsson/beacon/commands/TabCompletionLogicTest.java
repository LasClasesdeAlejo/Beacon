package com.colsson.beacon.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la lógica de tab completion
 * (extraída de PaperCommandExecutor para ser testeable sin Paper).
 */
class TabCompletionLogicTest {

    private List<String> completions;

    @BeforeEach
    void setUp() {
        completions = new ArrayList<>();
    }

    @Test
    void firstArg_completesTopLevelCommands() {
        String[] args = {"u"};
        addCompletions(args);
        assertTrue(completions.contains("user"));
        assertFalse(completions.contains("help"));
    }

    @Test
    void firstArg_completesAllTopLevel() {
        String[] args = {""};
        addCompletions(args);
        assertTrue(completions.contains("help"));
        assertTrue(completions.contains("info"));
        assertTrue(completions.contains("reload"));
        assertTrue(completions.contains("user"));
        assertTrue(completions.contains("group"));
        assertTrue(completions.contains("permission"));
        assertTrue(completions.contains("check"));
        assertTrue(completions.contains("groups"));
        assertTrue(completions.contains("debug"));
        assertTrue(completions.contains("history"));
    }

    @Test
    void userGroup_completesAddRemove() {
        String[] args = {"user", "player", "group", "a"};
        addCompletions(args);
        assertTrue(completions.contains("add"));
        assertFalse(completions.contains("remove"));
    }

    @Test
    void userPermission_completesSetRemoveClear() {
        String[] args = {"user", "player", "permission", "c"};
        addCompletions(args);
        assertTrue(completions.contains("clear"));
        assertFalse(completions.contains("set"));
    }

    @Test
    void groupEdit_completesFields() {
        String[] args = {"group", "vip", "edit", "p"};
        addCompletions(args);
        assertTrue(completions.contains("priority"));
        assertFalse(completions.contains("name"));
    }

    @Test
    void groupParent_completesSetRemove() {
        String[] args = {"group", "vip", "parent", "s"};
        addCompletions(args);
        assertTrue(completions.contains("set"));
        assertFalse(completions.contains("remove"));
    }

    @Test
    void groupPermission_completesActions() {
        String[] args = {"group", "vip", "permission", "r"};
        addCompletions(args);
        assertTrue(completions.contains("remove"));
        assertFalse(completions.contains("set"));
    }

    @Test
    void history_completesTypes() {
        String[] args = {"history", "u"};
        addCompletions(args);
        assertTrue(completions.contains("user"));
        assertFalse(completions.contains("group"));
    }

    @Test
    void debug_completesSubCommands() {
        String[] args = {"debug", "u"};
        addCompletions(args);
        assertTrue(completions.contains("user"));
        assertFalse(completions.contains("permission"));
    }

    @Test
    void groups_completesTree() {
        String[] args = {"groups", "t"};
        addCompletions(args);
        assertTrue(completions.contains("tree"));
    }

    // ── Helper (mismo lógico que PaperCommandExecutor) ──────

    private void addCompletions(String[] args) {
        if (args.length == 1) {
            completions.addAll(List.of(
                "help", "info", "reload", "user", "group",
                "permission", "check", "groups", "debug", "history"
            ));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "user" -> completions.add("<jugador>");
                case "group" -> {
                    completions.addAll(List.of("list", "create"));
                    completions.add("<nombre>");
                }
                case "permission" -> completions.addAll(List.of("list", "info", "search"));
                case "check" -> completions.add("<jugador>");
                case "debug" -> completions.addAll(List.of("user", "permission"));
                case "groups" -> completions.add("tree");
                case "history" -> completions.addAll(List.of("user", "group", "permission"));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "user" -> completions.addAll(List.of("info", "groups", "permissions", "group", "permission"));
                case "group" -> completions.addAll(List.of("info", "delete", "edit", "parent", "permission", "parents"));
                case "permission" -> completions.add("<permiso>");
                case "check" -> completions.add("<permiso>");
                case "debug" -> {
                    switch (args[1].toLowerCase()) {
                        case "user" -> completions.add("<jugador>");
                        case "permission" -> completions.add("<jugador>");
                    }
                }
                case "history" -> completions.add("<targeto>");
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "user" -> {
                    switch (args[2].toLowerCase()) {
                        case "group" -> completions.addAll(List.of("add", "remove"));
                        case "permission" -> completions.addAll(List.of("set", "remove", "clear"));
                    }
                }
                case "group" -> {
                    switch (args[2].toLowerCase()) {
                        case "edit" -> completions.addAll(List.of("name", "priority", "description"));
                        case "parent" -> completions.addAll(List.of("set", "remove"));
                        case "permission" -> completions.addAll(List.of("set", "remove", "clear"));
                    }
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        completions = completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .toList();
    }
}
