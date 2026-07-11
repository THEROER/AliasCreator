package dev.ua.theroer.commandflow.bukkit;

import java.util.UUID;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.commandflow.common.remote.RemoteCommand;
import dev.ua.theroer.commandflow.common.remote.RemoteReceiver;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.placeholders.MagicPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Backend-side execution of forwarded commands and placeholder queries on Bukkit.
 *
 * <p>Rebuilds a {@link MagicSender} for the forwarded player (or console) from
 * the payload and runs the command through the shared {@link ActionPlatform}, and
 * resolves query placeholders in the target player's context via MagicUtils
 * placeholders. Implements {@link RemoteReceiver.CommandExecutor} so it plugs
 * straight into {@link RemoteReceiver#install}.</p>
 */
public final class BukkitRemoteExecutor implements RemoteReceiver.CommandExecutor {
    private final ActionPlatform platform;

    /**
     * Creates the executor.
     *
     * @param platform platform effect adapter used to dispatch commands
     */
    public BukkitRemoteExecutor(ActionPlatform platform) {
        this.platform = platform;
    }

    @Override
    public void execute(RemoteCommand command) {
        // Run on the main thread: plugin messages arrive on the netty thread.
        Bukkit.getScheduler().runTask(pluginOwner(), () -> {
            MagicSender sender = resolveSender(command.playerUuid());
            if (sender == null) {
                return; // targeted player not on this backend; drop
            }
            RunAs runAs = parseRunAs(command.getExecutor());
            platform.dispatch(sender, runAs, command.getCommandLine());
        });
    }

    /**
     * Resolves a placeholder for a player in this server's context.
     *
     * @param playerId player id (may be null → server context)
     * @param placeholder placeholder string
     * @return resolved value (never null)
     */
    public String resolvePlaceholder(@Nullable UUID playerId, String placeholder) {
        var audience = audienceFor(playerId);
        String result = MagicPlaceholders.render(audience, placeholder, java.util.Map.of());
        return result != null ? result : "";
    }

    private @Nullable MagicSender resolveSender(@Nullable UUID playerId) {
        if (playerId == null) {
            return MagicSender.wrap(Bukkit.getConsoleSender());
        }
        Player player = Bukkit.getPlayer(playerId);
        return player != null ? MagicSender.wrap(player) : null;
    }

    private static dev.ua.theroer.magicutils.platform.@Nullable Audience audienceFor(@Nullable UUID playerId) {
        if (playerId == null) {
            return null;
        }
        Player player = Bukkit.getPlayer(playerId);
        return player != null ? MagicSender.wrap(player).audience() : null;
    }

    private static RunAs parseRunAs(String name) {
        if (name == null || name.isBlank()) {
            return RunAs.PLAYER;
        }
        try {
            return RunAs.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return RunAs.PLAYER;
        }
    }

    private static org.bukkit.plugin.Plugin pluginOwner() {
        return org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(BukkitRemoteExecutor.class);
    }
}
