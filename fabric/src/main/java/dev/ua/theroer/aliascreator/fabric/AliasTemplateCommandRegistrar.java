package dev.ua.theroer.aliascreator.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.ua.theroer.aliascreator.common.AliasCommandLine;
import dev.ua.theroer.aliascreator.common.AliasTemplateResolver;
import dev.ua.theroer.aliascreator.common.config.AliasTemplateArgument;
import dev.ua.theroer.aliascreator.common.config.AliasTemplateEntry;
import dev.ua.theroer.magicutils.Logger;
import dev.ua.theroer.magicutils.commands.CommandArgument;
import dev.ua.theroer.magicutils.commands.CommandExecution;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.CommandSpec;
import dev.ua.theroer.magicutils.commands.MagicPermissionDefault;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

final class AliasTemplateCommandRegistrar {
    private static final String CHILDREN_FIELD = "children";
    private static final String LITERALS_FIELD = "literals";
    private static final String ARGUMENTS_FIELD = "arguments";

    private final String modName;
    private final String permissionPrefix;
    private final Logger magicLogger;
    private final int logLevel;
    private final Supplier<MinecraftServer> serverSupplier;
    private CommandDispatcher<ServerCommandSource> dispatcher;
    private CommandRegistry templateRegistry;
    private Set<String> registered = new HashSet<>();

    AliasTemplateCommandRegistrar(String modName,
                                  String permissionPrefix,
                                  Logger magicLogger,
                                  int logLevel,
                                  Supplier<MinecraftServer> serverSupplier) {
        this.modName = modName;
        this.permissionPrefix = permissionPrefix;
        this.magicLogger = magicLogger;
        this.logLevel = logLevel;
        this.serverSupplier = serverSupplier;
    }

    void registerDispatcher(CommandDispatcher<ServerCommandSource> dispatcher) {
        this.dispatcher = dispatcher;
    }

    void applyTemplates(List<AliasTemplateEntry> templates) {
        if (dispatcher == null) {
            return;
        }
        unregisterAll();

        templateRegistry = CommandRegistry.create(modName, permissionPrefix, magicLogger, logLevel);
        if (templates == null) {
            return;
        }

        Set<String> aliases = new HashSet<>();
        for (AliasTemplateEntry entry : templates) {
            if (entry == null) {
                continue;
            }
            String alias = AliasCommandLine.normalizeAlias(entry.getAlias());
            if (alias.isEmpty()) {
                continue;
            }
            if (dispatcher.getRoot().getChild(alias) != null) {
                magicLogger.warn("Template alias '" + alias + "' already exists, skipping.");
                continue;
            }
            templateRegistry.registerSpec(dispatcher, buildSpec(alias, entry));
            aliases.add(alias);
        }
        registered = aliases;
        refreshCommandTree();
    }

    private void unregisterAll() {
        if (dispatcher == null) {
            return;
        }
        for (String alias : registered) {
            removeNode(dispatcher, alias);
        }
        registered = new HashSet<>();
    }

    private CommandSpec<ServerCommandSource> buildSpec(String alias, AliasTemplateEntry entry) {
        CommandSpec.Builder<ServerCommandSource> builder = CommandSpec.builder(alias);
        String permission = normalizePermission(entry.getPermission());
        MagicPermissionDefault defaultValue = permission.isEmpty()
                ? MagicPermissionDefault.TRUE
                : MagicPermissionDefault.OP;
        builder.permission(permission);
        builder.permissionDefault(defaultValue);

        List<CommandArgument> arguments = new ArrayList<>();
        List<AliasTemplateArgument> templateArgs = entry.getArgs();
        if (templateArgs != null) {
            for (AliasTemplateArgument arg : templateArgs) {
                if (arg == null) {
                    continue;
                }
                CommandArgument.Builder argBuilder = CommandArgument.builder(normalizeName(arg), String.class);
                if (arg.isGreedy()) {
                    argBuilder.greedy();
                }
                if (arg.getDefaultValue() != null && !arg.getDefaultValue().isBlank()) {
                    argBuilder.defaultValue(arg.getDefaultValue());
                } else if (arg.isOptional()) {
                    argBuilder.optional();
                }
                String suggestionSource = buildSuggestionSource(arg);
                if (suggestionSource != null) {
                    argBuilder.suggestions(suggestionSource);
                }
                arguments.add(argBuilder.build());
            }
        }
        builder.arguments(arguments);
        builder.execute(execution -> executeTemplate(execution, entry));
        return builder.build();
    }

    private CommandResult executeTemplate(CommandExecution<ServerCommandSource> execution,
                                          AliasTemplateEntry entry) {
        ServerCommandSource source = execution.sender();
        String[] inputArgs = execution.rawArgs().toArray(new String[0]);
        AliasTemplateResolver.Result result = AliasTemplateResolver.resolve(entry, inputArgs, source.getName());
        if (!result.isSuccess()) {
            return CommandResult.failure(result.errorMessage(), false);
        }
        source.getServer().getCommandManager().parseAndExecute(source, result.commandLine());
        return CommandResult.success();
    }

    private void refreshCommandTree() {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            server.getCommandManager().sendCommandTree(player);
        }
    }

    private static void removeNode(CommandDispatcher<ServerCommandSource> dispatcher, String alias) {
        try {
            Object root = dispatcher.getRoot();
            if (!(root instanceof CommandNode<?> node)) {
                return;
            }
            Field children = CommandNode.class.getDeclaredField(CHILDREN_FIELD);
            Field literals = CommandNode.class.getDeclaredField(LITERALS_FIELD);
            Field arguments = CommandNode.class.getDeclaredField(ARGUMENTS_FIELD);
            children.setAccessible(true);
            literals.setAccessible(true);
            arguments.setAccessible(true);

            String key = alias.toLowerCase(Locale.ROOT);
            ((Map<?, ?>) children.get(node)).remove(key);
            ((Map<?, ?>) literals.get(node)).remove(key);
            ((Map<?, ?>) arguments.get(node)).remove(key);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static String normalizePermission(String permission) {
        if (permission == null) {
            return "";
        }
        String trimmed = permission.trim();
        return trimmed.equals("-") ? "" : trimmed;
    }

    private static String normalizeName(AliasTemplateArgument arg) {
        String name = arg.getName();
        if (name == null || name.isBlank()) {
            return "arg";
        }
        return name.trim();
    }

    private static String buildSuggestionSource(AliasTemplateArgument arg) {
        Set<String> suggestions = new LinkedHashSet<>();
        if (arg.getValues() != null) {
            for (String value : arg.getValues()) {
                if (value != null && !value.isBlank()) {
                    suggestions.add(value);
                }
            }
        }
        if (arg.getAliases() != null) {
            for (String alias : arg.getAliases().keySet()) {
                if (alias != null && !alias.isBlank()) {
                    suggestions.add(alias);
                }
            }
        }
        if (suggestions.isEmpty()) {
            return null;
        }
        return "{" + String.join(", ", suggestions) + "}";
    }
}
