package dev.ua.theroer.commandflow.bungee;

import java.util.List;

import dev.ua.theroer.commandflow.common.AliasCommandLine;
import dev.ua.theroer.commandflow.common.ProxyForwarder;
import dev.ua.theroer.commandflow.common.config.AliasEntry;
import dev.ua.theroer.magicutils.commands.CommandArgument;
import dev.ua.theroer.magicutils.commands.CommandRegistry;
import dev.ua.theroer.magicutils.commands.CommandResult;
import dev.ua.theroer.magicutils.commands.MagicCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Registers CommandFlow aliases on a BungeeCord/Waterfall proxy as thin
 * forwarders to the player's backend, via the MagicUtils {@link CommandRegistry}
 * and the shared {@link ProxyForwarder}.
 */
public final class BungeeProxyAliasRegistrar {
    private final CommandRegistry commandRegistry;
    private final ProxyForwarder forwarder;

    /**
     * Creates the registrar.
     *
     * @param commandRegistry proxy command registry
     * @param forwarder shared forward logic
     */
    public BungeeProxyAliasRegistrar(CommandRegistry commandRegistry, ProxyForwarder forwarder) {
        this.commandRegistry = commandRegistry;
        this.forwarder = forwarder;
    }

    /**
     * Registers a forwarding command for every configured alias.
     *
     * @param entries alias entries
     */
    public void applyAliases(List<AliasEntry> entries) {
        if (entries == null) {
            return;
        }
        for (AliasEntry entry : entries) {
            if (entry == null || entry.getAliases() == null) {
                continue;
            }
            for (String alias : entry.getAliases()) {
                String name = AliasCommandLine.normalizeAlias(alias);
                if (!name.isEmpty()) {
                    register(name, entry.getPermission());
                }
            }
        }
    }

    private void register(String alias, String permission) {
        CommandArgument args = CommandArgument.builder("args", String.class)
                .greedy()
                .optional()
                .build();
        MagicCommand command = MagicCommand.<CommandSender>builder(alias)
                .permission(permission != null && !permission.isBlank() ? permission : null)
                .argument(args)
                .execute(execution -> forward(alias, execution.sender(), execution.rawArgs()))
                .build();
        commandRegistry.registerCommand(command);
    }

    private CommandResult forward(String alias, CommandSender sender, List<String> rawArgs) {
        ProxyForwarder.Outcome outcome = forwarder.forward(alias, sender, rawArgs);
        return switch (outcome) {
            case SENT -> CommandResult.success();
            case NOT_A_PLAYER -> deny(sender, "Cross-server aliases can only be used by players.");
            case UNREACHABLE -> deny(sender, "Could not reach your server.");
        };
    }

    private CommandResult deny(CommandSender sender, String message) {
        sender.sendMessage(new TextComponent(message));
        return CommandResult.failure(false);
    }
}
