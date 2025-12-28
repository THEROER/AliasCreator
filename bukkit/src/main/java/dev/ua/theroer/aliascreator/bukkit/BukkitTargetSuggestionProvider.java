package dev.ua.theroer.aliascreator.bukkit;

import dev.ua.theroer.aliascreator.common.CommandNameSelector;
import dev.ua.theroer.aliascreator.common.TargetSuggestionProvider;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

public final class BukkitTargetSuggestionProvider implements TargetSuggestionProvider {
    private final BooleanSupplier alwaysNamespaced;

    public BukkitTargetSuggestionProvider(BooleanSupplier alwaysNamespaced) {
        this.alwaysNamespaced = alwaysNamespaced != null ? alwaysNamespaced : () -> false;
    }

    @Override
    public List<String> getSuggestions() {
        List<String> raw = new ArrayList<>();
        Map<String, Object> identities = new LinkedHashMap<>();
        collectFromTabComplete(raw);
        collectFromCommandMap(raw, identities);
        return CommandNameSelector.select(raw, identities, alwaysNamespaced.getAsBoolean());
    }

    private void collectFromTabComplete(List<String> suggestions) {
        Server server = Bukkit.getServer();
        if (server == null) {
            return;
        }
        CommandSender sender = Bukkit.getConsoleSender();
        List<String> completed = server.getCommandMap().tabComplete(sender, "/");
        if (completed == null) {
            return;
        }
        suggestions.addAll(completed);
    }

    private void collectFromCommandMap(List<String> suggestions, Map<String, Object> identities) {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            return;
        }
        Map<String, Command> knownCommands = getKnownCommands(commandMap);
        if (knownCommands == null || knownCommands.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.isEmpty()) {
                continue;
            }
            suggestions.add(name);
            identities.put(name, entry.getValue());
        }
    }

    private CommandMap getCommandMap() {
        Server server = Bukkit.getServer();
        if (server == null) {
            return null;
        }
        try {
            Method method = server.getClass().getMethod("getCommandMap");
            Object result = method.invoke(server);
            if (result instanceof CommandMap) {
                return (CommandMap) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            Object result = field.get(server);
            if (result instanceof CommandMap) {
                return (CommandMap) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Command> getKnownCommands(CommandMap commandMap) {
        try {
            Method method = commandMap.getClass().getMethod("getKnownCommands");
            Object result = method.invoke(commandMap);
            if (result instanceof Map) {
                return (Map<String, Command>) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Field field = commandMap.getClass().getDeclaredField("knownCommands");
            field.setAccessible(true);
            Object result = field.get(commandMap);
            if (result instanceof Map) {
                return (Map<String, Command>) result;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
