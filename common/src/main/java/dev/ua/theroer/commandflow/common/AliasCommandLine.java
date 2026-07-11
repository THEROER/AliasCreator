package dev.ua.theroer.commandflow.common;

import java.util.Locale;

public final class AliasCommandLine {
    private AliasCommandLine() {
    }

    public static String normalizeAlias(String alias) {
        if (alias == null) {
            return "";
        }
        return alias.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeTarget(String target) {
        if (target == null) {
            return "";
        }
        String trimmed = target.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    public static String buildCommandLine(String target, String[] args) {
        String base = normalizeTarget(target);
        if (args == null || args.length == 0) {
            return base;
        }
        String joined = String.join(" ", args);
        if (base.isEmpty()) {
            return joined;
        }
        return base + " " + joined;
    }

    public static String buildCommandLine(String target, String args) {
        String base = normalizeTarget(target);
        String tail = args != null ? args.trim() : "";
        if (tail.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return tail;
        }
        return base + " " + tail;
    }

    public static String[] splitArgs(String args) {
        if (args == null) {
            return new String[0];
        }
        String trimmed = args.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }
}
