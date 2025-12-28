package dev.ua.theroer.aliascreator.common;

import dev.ua.theroer.aliascreator.common.config.AliasEntry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AliasService {
    public record AliasTarget(String target, String permission) {
    }

    private final Map<String, AliasTarget> aliases = new ConcurrentHashMap<>();

    public void registerAlias(String alias, AliasTarget target) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        if (target == null || target.target == null || target.target.isBlank()) {
            throw new IllegalArgumentException("target must not be blank");
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
            AliasTarget target = new AliasTarget(entry.getTarget(), entry.getPermission());
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
