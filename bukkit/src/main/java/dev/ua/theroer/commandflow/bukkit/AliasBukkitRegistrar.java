package dev.ua.theroer.commandflow.bukkit;

import dev.ua.theroer.commandflow.common.AliasCommandLine;
import dev.ua.theroer.commandflow.common.AliasService;
import dev.ua.theroer.commandflow.common.CommandGate;
import dev.ua.theroer.commandflow.common.action.ActionContext;
import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.ActionSequenceExecutor;
import dev.ua.theroer.commandflow.common.config.AliasEntry;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;
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
import java.util.function.Function;

final class AliasBukkitRegistrar {
    private final Plugin plugin;
    private final AliasService aliasService;
    private final ActionPlatform actionPlatform;
    private final CommandGate gate;
    private final RemoteDispatcher remote;
    private final Function<String, Component> textParser;
    private final CommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private final Set<String> registered = new HashSet<>();

    AliasBukkitRegistrar(Plugin plugin, AliasService aliasService, ActionPlatform actionPlatform,
                         CommandGate gate, Function<String, Component> textParser, RemoteDispatcher remote) {
        this.plugin = plugin;
        this.aliasService = aliasService;
        this.actionPlatform = actionPlatform;
        this.gate = gate;
        this.textParser = textParser;
        this.remote = remote;
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
                plugin.getLogger().warning("Alias '" + alias + "' already exists, skipping.");
                continue;
            }
            AliasForwardCommand command = new AliasForwardCommand(alias, plugin, aliasService,
                    actionPlatform, gate, textParser, remote);
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
        Class<?> current = map.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Command>) field.get(map);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException e) {
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    private static final class AliasForwardCommand extends Command implements PluginIdentifiableCommand {
        private final Plugin plugin;
        private final AliasService aliasService;
        private final ActionPlatform actionPlatform;
        private final CommandGate gate;
        private final Function<String, Component> textParser;
        private final RemoteDispatcher remote;

        private AliasForwardCommand(String name, Plugin plugin, AliasService aliasService,
                                    ActionPlatform actionPlatform, CommandGate gate,
                                    Function<String, Component> textParser, RemoteDispatcher remote) {
            super(name);
            this.plugin = plugin;
            this.aliasService = aliasService;
            this.actionPlatform = actionPlatform;
            this.gate = gate;
            this.textParser = textParser;
            this.remote = remote;
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

            MagicSender magicSender = MagicSender.wrap(sender);
            Map<String, String> placeholders = placeholders(magicSender, args);
            if (!gate.check(commandLabel, target.entry(), magicSender, placeholders, actionPlatform)) {
                return true;
            }

            if (target.hasActions()) {
                ActionContext context = new ActionContext(
                        magicSender, actionPlatform, placeholders, textParser, remote);
                ActionSequenceExecutor.run(target.actions(), context);
                return true;
            }

            String commandLine = AliasCommandLine.buildCommandLine(target.target(), args);
            if (commandLine.isEmpty()) {
                sender.sendMessage("Alias target is empty");
                return true;
            }
            return Bukkit.dispatchCommand(sender, commandLine);
        }

        private static Map<String, String> placeholders(MagicSender sender, String[] args) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("sender", sender.name());
            placeholders.put("args", String.join(" ", args));
            return placeholders;
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
