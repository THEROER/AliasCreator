package dev.ua.theroer.aliascreator.common;

import dev.ua.theroer.aliascreator.common.config.AliasTemplateEntry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AliasTemplateService {
    private final Map<String, AliasTemplateEntry> templates = new ConcurrentHashMap<>();

    public void replace(List<AliasTemplateEntry> entries) {
        templates.clear();
        registerAll(entries);
    }

    public void registerAll(List<AliasTemplateEntry> entries) {
        if (entries == null) {
            return;
        }
        for (AliasTemplateEntry entry : entries) {
            if (entry == null || entry.getAlias() == null) {
                continue;
            }
            String alias = AliasCommandLine.normalizeAlias(entry.getAlias());
            if (alias.isEmpty()) {
                continue;
            }
            templates.put(alias.toLowerCase(Locale.ROOT), entry);
        }
    }

    public AliasTemplateEntry resolve(String alias) {
        if (alias == null) {
            return null;
        }
        return templates.get(AliasCommandLine.normalizeAlias(alias));
    }

    public Map<String, AliasTemplateEntry> snapshot() {
        return Map.copyOf(templates);
    }
}
