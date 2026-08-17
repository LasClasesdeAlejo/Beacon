package com.colsson.beacon.commands;

/**
 * Utilidades de formato para mensajes de Beacon.
 * Paper-independiente: usa códigos de color §.
 */
public final class MessageHelper {

    public static final String PREFIX = "§6[Beacon] §r";
    public static final String PREFIX_ERROR = "§c[Beacon] §r";
    public static final String PREFIX_SUCCESS = "§a[Beacon] §r";
    public static final String PREFIX_INFO = "§e[Beacon] §r";

    private MessageHelper() {}

    public static String prefix(String message) {
        return PREFIX + message;
    }

    public static String error(String message) {
        return PREFIX_ERROR + message;
    }

    public static String success(String message) {
        return PREFIX_SUCCESS + message;
    }

    public static String info(String message) {
        return PREFIX_INFO + message;
    }

    public static String header(String title) {
        return "§6§l━━ " + title + " §6§l━━";
    }

    public static String keyValue(String key, String value) {
        return "§7" + key + ": §f" + value;
    }

    public static String permissionState(String permission, String state, String color) {
        return "§7" + permission + ": " + color + state;
    }
}
