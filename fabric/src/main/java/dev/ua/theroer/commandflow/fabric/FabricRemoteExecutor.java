package dev.ua.theroer.commandflow.fabric;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.commandflow.common.remote.RemoteCommand;
import dev.ua.theroer.commandflow.common.remote.RemoteReceiver;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.placeholders.MagicPlaceholders;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Backend-side execution of forwarded commands and placeholder queries on Fabric.
 *
 * <p>Rebuilds a command source for the forwarded player (or the server console)
 * and dispatches through the shared {@link ActionPlatform}; resolves query
 * placeholders in the target player's context via MagicUtils placeholders.
 * Cross-server messaging on Fabric requires Redis (there is no mod plugin-message
 * channel), so this only ever runs when Redis is enabled.</p>
 */
public final class FabricRemoteExecutor implements RemoteReceiver.CommandExecutor {
    private final Supplier<MinecraftServer> serverSupplier;
    private final ActionPlatform platform;

    /**
     * Creates the executor.
     *
     * @param serverSupplier running server supplier
     * @param platform platform effect adapter used to dispatch commands
     */
    public FabricRemoteExecutor(Supplier<MinecraftServer> serverSupplier, ActionPlatform platform) {
        this.serverSupplier = serverSupplier;
        this.platform = platform;
    }

    @Override
    public void execute(RemoteCommand command) {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return;
        }
        // Redis subscription delivers off-thread; hop to the server thread.
        server.execute(() -> {
            MagicSender sender = resolveSender(server, command.playerUuid());
            if (sender == null) {
                return; // targeted player not on this backend; drop
            }
            platform.dispatch(sender, parseRunAs(command.getExecutor()), command.getCommandLine());
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
        MinecraftServer server = serverSupplier.get();
        var audience = server != null ? audienceFor(server, playerId) : null;
        String result = MagicPlaceholders.render(audience, placeholder, Map.of());
        return result != null ? result : "";
    }

    private static @Nullable MagicSender resolveSender(MinecraftServer server, @Nullable UUID playerId) {
        if (playerId == null) {
            return MagicSender.wrap(server.createCommandSourceStack());
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player != null ? MagicSender.wrap(player.createCommandSourceStack()) : null;
    }

    private static dev.ua.theroer.magicutils.platform.@Nullable Audience audienceFor(
            MinecraftServer server, @Nullable UUID playerId) {
        if (playerId == null) {
            return null;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return null;
        }
        CommandSourceStack source = player.createCommandSourceStack();
        return MagicSender.wrap(source).audience();
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
}
