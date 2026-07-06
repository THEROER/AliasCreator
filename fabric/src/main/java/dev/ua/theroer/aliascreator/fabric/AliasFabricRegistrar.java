package dev.ua.theroer.aliascreator.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;

import dev.ua.theroer.aliascreator.common.AliasCommandLine;
import dev.ua.theroer.aliascreator.common.AliasService;
import dev.ua.theroer.aliascreator.common.config.AliasEntry;
import dev.ua.theroer.magicutils.commands.FabricCommandPlatform;
import dev.ua.theroer.magicutils.commands.MagicPermissionDefault;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class AliasFabricRegistrar {
    private static final String CHILDREN_FIELD = "children";
    private static final String LITERALS_FIELD = "literals";
    private static final String ARGUMENTS_FIELD = "arguments";

    private final AliasService aliasService;
    private final Supplier<MinecraftServer> serverSupplier;
    private final FabricCommandPlatform permissionPlatform;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private Set<String> registered = new HashSet<>();

    AliasFabricRegistrar(AliasService aliasService, Supplier<MinecraftServer> serverSupplier) {
        this.aliasService = aliasService;
        this.serverSupplier = serverSupplier;
        this.permissionPlatform = new FabricCommandPlatform();
    }

    void registerDispatcher(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    void applyAliases(List<AliasEntry> entries) {
        if (dispatcher == null) {
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
            registerAlias(dispatcher, alias);
        }
        registered = aliases;
        refreshCommandTree();
    }

    void unregisterAll() {
        if (dispatcher == null) {
            return;
        }
        for (String alias : registered) {
            removeNode(dispatcher, alias);
        }
        registered = new HashSet<>();
    }

    private void registerAlias(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
        if (dispatcher.getRoot().getChild(alias) != null) {
            return;
        }

        LiteralArgumentBuilder<CommandSourceStack> root = LiteralArgumentBuilder.literal(alias);
        root.executes(context -> execute(alias, context.getSource(), ""));

        RequiredArgumentBuilder<CommandSourceStack, String> argsNode = RequiredArgumentBuilder
                .<CommandSourceStack, String>argument("args", StringArgumentType.greedyString())
                .suggests((context, builder) -> suggest(alias, context, builder))
                .executes(context -> execute(alias, context.getSource(),
                        StringArgumentType.getString(context, "args")));
        root.then(argsNode);

        dispatcher.register(root);
    }

    private int execute(String alias, CommandSourceStack source, String args) {
        AliasService.AliasTarget target = aliasService.resolve(alias);
        if (target == null) {
            source.sendFailure(Component.literal("Unknown alias"));
            return 0;
        }
        if (!hasPermission(source, target.permission())) {
            source.sendFailure(Component.literal("You do not have permission to use this alias"));
            return 0;
        }

        String commandLine = AliasCommandLine.buildCommandLine(target.target(), args);
        if (commandLine.isEmpty()) {
            source.sendFailure(Component.literal("Alias target is empty"));
            return 0;
        }

        Commands manager = getCommands();
        if (manager == null) {
            return 0;
        }
        manager.performPrefixedCommand(source, commandLine);
        return 1;
    }

    private CompletableFuture<Suggestions> suggest(String alias, CommandContext<CommandSourceStack> context,
                                                   SuggestionsBuilder builder) {
        AliasService.AliasTarget target = aliasService.resolve(alias);
        if (target == null) {
            return builder.buildFuture();
        }
        if (!hasPermission(context.getSource(), target.permission())) {
            return builder.buildFuture();
        }

        CommandDispatcher<CommandSourceStack> localDispatcher = dispatcher;
        if (localDispatcher == null) {
            return builder.buildFuture();
        }
        String commandLine = AliasCommandLine.buildCommandLine(target.target(), builder.getRemaining());
        if (commandLine.isEmpty()) {
            return builder.buildFuture();
        }

        return localDispatcher.getCompletionSuggestions(localDispatcher.parse(commandLine, context.getSource()))
                .thenApply(suggestions -> {
                    suggestions.getList().forEach(suggestion -> builder.suggest(suggestion.getText()));
                    return builder.build();
                });
    }

    private boolean hasPermission(CommandSourceStack source, String permission) {
        if (permission == null || permission.isBlank()) {
            return true;
        }
        return permissionPlatform.hasPermission(source, permission.trim(), MagicPermissionDefault.OP);
    }

    private void refreshCommandTree() {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            server.getCommands().sendCommands(player);
        }
    }

    private static void removeNode(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
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

    private Commands getCommands() {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return null;
        }
        return server.getCommands();
    }
}
