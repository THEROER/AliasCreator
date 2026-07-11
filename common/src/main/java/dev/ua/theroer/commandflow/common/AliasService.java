package dev.ua.theroer.commandflow.common;

import dev.ua.theroer.commandflow.common.config.ActionEntry;
import dev.ua.theroer.commandflow.common.config.AliasEntry;
import dev.ua.theroer.commandflow.common.config.CommandEntry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AliasService {
    /**
     * A resolved alias destination. Carries the originating {@link CommandEntry}
     * (conditions, cooldown, messages) plus convenience accessors for its target
     * command and action sequence.
     */
    public record AliasTarget(CommandEntry entry) {
        public AliasTarget {
            if (entry == null) {
                throw new IllegalArgumentException("entry must not be null");
            }
        }

        public String target() {
            return entry.getTarget();
        }

        public String permission() {
            return entry.getPermission();
        }

        public List<ActionEntry> actions() {
            return entry.getActions() != null ? entry.getActions() : List.of();
        }

        /**
         * @return true when this alias runs an action sequence instead of (or in
         *         addition to) a single target command
         */
        public boolean hasActions() {
            return !actions().isEmpty();
        }
    }

    private final Map<String, AliasTarget> aliases = new ConcurrentHashMap<>();

    public void registerAlias(String alias, AliasTarget target) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        boolean hasTarget = target.target() != null && !target.target().isBlank();
        if (!hasTarget && !target.hasActions()) {
            throw new IllegalArgumentException("alias needs a target command or an action sequence");
        }
        aliases.put(normalize(alias), target);
    }

    public void registerAll(List<AliasEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (AliasEntry entry : entries) {
            if (entry == null || entry.getAliases() == null || entry.getAliases().isEmpty()) {
                continue;
            }
            AliasTarget target = new AliasTarget(entry);
            for (String alias : entry.getAliases()) {
                if (alias == null || alias.isBlank()) {
                    continue;
                }
                registerAlias(alias, target);
            }
        }
    }

    public void replace(List<AliasEntry> entries) {
        aliases.clear();
        registerAll(entries);
    }

    public AliasTarget resolve(String alias) {
        if (alias == null) {
            return null;
        }
        return aliases.get(normalize(alias));
    }

    public Map<String, AliasTarget> snapshot() {
        return Map.copyOf(aliases);
    }

    private String normalize(String alias) {
        return alias.trim().toLowerCase(Locale.ROOT);
    }
}
