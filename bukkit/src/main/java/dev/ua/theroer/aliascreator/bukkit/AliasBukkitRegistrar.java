package dev.ua.theroer.aliascreator.bukkit;

import dev.ua.theroer.aliascreator.common.AliasCommandLine;
import dev.ua.theroer.aliascreator.common.AliasService;
import dev.ua.theroer.aliascreator.common.config.AliasEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class AliasBukkitRegistrar {
    private final Plugin plugin;
    private final AliasService aliasService;
    private final CommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private final Set<String> registered = new HashSet<>();

    AliasBukkitRegistrar(Plugin plugin, AliasService aliasService) {
        this.plugin = plugin;
        this.aliasService = aliasService;
        this.commandMap = resolveCommandMap();
        this.knownCommands = resolveKnownCommands(commandMap);
    }

    void applyAliases(List<AliasEntry> entries) {
        if (commandMap == null || knownCommands == null) {
            return;
        }
        unregisterAll();

        Set<String> aliases = new HashSet<>();
        if (entries != null) {
            for (AliasEntry entry : entries) {
                if (entry == null || entry.getAliases() == null) {
                    continue;
                }
                for (String alias : entry.getAliases()) {
                    String normalized = AliasCommandLine.normalizeAlias(alias);
                    if (!normalized.isEmpty()) {
                        aliases.add(normalized);
                    }
                }
            }
        }

        for (String alias : aliases) {
            if (knownCommands.containsKey(alias)) {
                Command existing = knownCommands.get(alias);
                if (!isOwned(existing)) {
                    plugin.getLogger().warning("Alias '" + alias + "' already exists, skipping.");
                    continue;
                }
            }
            AliasForwardCommand command = new AliasForwardCommand(alias, plugin, aliasService);
            commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), command);
            registered.add(alias);
        }
    }

    void unregisterAll() {
        if (commandMap == null || knownCommands == null) {
            return;
        }
        for (String alias : new HashSet<>(registered)) {
            unregister(alias);
        }
        registered.clear();
    }

    private void unregister(String alias) {
        knownCommands.remove(alias);
        knownCommands.remove(plugin.getName().toLowerCase(Locale.ROOT) + ":" + alias);
    }

    private boolean isOwned(Command command) {
        if (command instanceof PluginIdentifiableCommand pic) {
            return pic.getPlugin() == plugin;
        }
        return command != null && command.getClass().getSimpleName().contains("AliasForwardCommand");
    }

    private static CommandMap resolveCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> resolveKnownCommands(CommandMap map) {
        if (map == null) {
            return null;
        }
        try {
            Field field = map.getClass().getDeclaredField("knownCommands");
            field.setAccessible(true);
            return (Map<String, Command>) field.get(map);
        } catch (ReflectiveOperationException e) {
            return new HashMap<>();
        }
    }

    private static final class AliasForwardCommand extends Command implements PluginIdentifiableCommand {
        private final Plugin plugin;
        private final AliasService aliasService;

        private AliasForwardCommand(String name, Plugin plugin, AliasService aliasService) {
            super(name);
            this.plugin = plugin;
            this.aliasService = aliasService;
            setDescription("Alias for another command");
        }

        @Override
        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
            AliasService.AliasTarget target = aliasService.resolve(commandLabel);
            if (target == null) {
                sender.sendMessage("Unknown alias");
                return true;
            }
            if (!hasPermission(sender, target.permission())) {
                sender.sendMessage("You do not have permission to use this alias");
                return true;
            }

            String commandLine = AliasCommandLine.buildCommandLine(target.target(), args);
            if (commandLine.isEmpty()) {
                sender.sendMessage("Alias target is empty");
                return true;
            }
            return Bukkit.dispatchCommand(sender, commandLine);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            AliasService.AliasTarget target = aliasService.resolve(alias);
            if (target == null) {
                return List.of();
            }
            if (!hasPermission(sender, target.permission())) {
                return List.of();
            }
            String commandLine = AliasCommandLine.buildCommandLine(target.target(), args);
            if (commandLine.isEmpty()) {
                return List.of();
            }
            return Bukkit.getServer().getCommandMap().tabComplete(sender, commandLine);
        }

        private boolean hasPermission(CommandSender sender, String permission) {
            if (permission == null || permission.isBlank()) {
                return true;
            }
            return sender.hasPermission(permission.trim());
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
