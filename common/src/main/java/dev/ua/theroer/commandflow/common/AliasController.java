package dev.ua.theroer.commandflow.common;

import dev.ua.theroer.commandflow.common.config.CommandFlowConfig;
import dev.ua.theroer.commandflow.common.config.AliasEntry;
import dev.ua.theroer.magicutils.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public final class AliasController {
    private final ConfigManager configManager;
    private final CommandFlowConfig config;
    private final AliasService aliasService;
    private final Runnable refresher;

    public AliasController(ConfigManager configManager, CommandFlowConfig config,
                           AliasService aliasService, Runnable refresher) {
        this.configManager = configManager;
        this.config = config;
        this.aliasService = aliasService;
        this.refresher = refresher;
    }

    public synchronized List<AliasEntry> entries() {
        List<AliasEntry> current = config.getAliases();
        return current != null ? new ArrayList<>(current) : new ArrayList<>();
    }

    public synchronized boolean setAlias(String alias, String target) {
        String normalizedAlias = AliasCommandLine.normalizeAlias(alias);
        String normalizedTarget = AliasCommandLine.normalizeTarget(target);
        if (normalizedAlias.isEmpty() || normalizedTarget.isEmpty()) {
            return false;
        }

        AliasEntry existing = findEntryByAlias(normalizedAlias);
        String permission = existing != null ? safePermission(existing.getPermission()) : "";
        removeAliasFromAll(normalizedAlias);

        AliasEntry targetEntry = findEntryByTargetAndPermission(normalizedTarget, permission);
        if (targetEntry == null) {
            targetEntry = new AliasEntry();
            targetEntry.setTarget(normalizedTarget);
            targetEntry.setPermission(permission);
            targetEntry.getAliases().add(normalizedAlias);
            config.getAliases().add(targetEntry);
        } else if (!containsAlias(targetEntry, normalizedAlias)) {
            targetEntry.getAliases().add(normalizedAlias);
        }

        saveAndRefresh();
        return true;
    }

    public synchronized boolean setPermission(String alias, String permission) {
        String normalizedAlias = AliasCommandLine.normalizeAlias(alias);
        if (normalizedAlias.isEmpty()) {
            return false;
        }
        AliasEntry existing = findEntryByAlias(normalizedAlias);
        if (existing == null) {
            return false;
        }

        String normalizedPermission = safePermission(permission);
        if (normalizedPermission.equals(safePermission(existing.getPermission()))) {
            return true;
        }

        String target = existing.getTarget();
        removeAliasFromAll(normalizedAlias);

        AliasEntry targetEntry = findEntryByTargetAndPermission(target, normalizedPermission);
        if (targetEntry == null) {
            targetEntry = new AliasEntry();
            targetEntry.setTarget(target);
            targetEntry.setPermission(normalizedPermission);
            targetEntry.getAliases().add(normalizedAlias);
            config.getAliases().add(targetEntry);
        } else if (!containsAlias(targetEntry, normalizedAlias)) {
            targetEntry.getAliases().add(normalizedAlias);
        }

        saveAndRefresh();
        return true;
    }

    public synchronized boolean removeAlias(String alias) {
        String normalizedAlias = AliasCommandLine.normalizeAlias(alias);
        if (normalizedAlias.isEmpty()) {
            return false;
        }
        AliasEntry existing = findEntryByAlias(normalizedAlias);
        if (existing == null) {
            return false;
        }
        removeAliasFromAll(normalizedAlias);
        saveAndRefresh();
        return true;
    }

    public synchronized void reload() {
        configManager.reload(config);
        aliasService.replace(config.getAliases());
        if (refresher != null) {
            refresher.run();
        }
    }

    private void saveAndRefresh() {
        configManager.save(config);
        aliasService.replace(config.getAliases());
        if (refresher != null) {
            refresher.run();
        }
    }

    private AliasEntry findEntryByAlias(String alias) {
        for (AliasEntry entry : safeEntries()) {
            if (containsAlias(entry, alias)) {
                return entry;
            }
        }
        return null;
    }

    private AliasEntry findEntryByTargetAndPermission(String target, String permission) {
        for (AliasEntry entry : safeEntries()) {
            if (target.equalsIgnoreCase(safeTarget(entry.getTarget()))
                    && permission.equalsIgnoreCase(safePermission(entry.getPermission()))) {
                return entry;
            }
        }
        return null;
    }

    private List<AliasEntry> safeEntries() {
        List<AliasEntry> list = config.getAliases();
        if (list == null) {
            list = new ArrayList<>();
            config.setAliases(list);
        }
        return list;
    }

    private boolean containsAlias(AliasEntry entry, String alias) {
        if (entry == null || entry.getAliases() == null) {
            return false;
        }
        for (String existing : entry.getAliases()) {
            if (existing != null && existing.trim().equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    private void removeAliasFromEntry(AliasEntry entry, String alias) {
        if (entry == null || entry.getAliases() == null) {
            return;
        }
        entry.getAliases().removeIf(existing -> existing != null
                && existing.trim().equalsIgnoreCase(alias));
        if (entry.getAliases().isEmpty()) {
            config.getAliases().remove(entry);
        }
    }

    private void removeAliasFromAll(String alias) {
        for (AliasEntry entry : new ArrayList<>(safeEntries())) {
            removeAliasFromEntry(entry, alias);
        }
    }

    private String safePermission(String permission) {
        if (permission == null) {
            return "";
        }
        String trimmed = permission.trim();
        if (trimmed.equals("-")) {
            return "";
        }
        return trimmed;
    }

    private String safeTarget(String target) {
        return target != null ? target.trim() : "";
    }
}
