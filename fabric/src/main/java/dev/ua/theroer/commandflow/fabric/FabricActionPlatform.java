package dev.ua.theroer.commandflow.fabric;

import java.util.function.Supplier;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.platform.fabric.FabricComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Fabric implementation of {@link ActionPlatform}.
 *
 * <p>Commands run through the server command dispatcher. Player-facing effects
 * convert Adventure components to native text via {@link FabricComponentSerializer}
 * and send titles/action bars as clientbound packets. Delays are scheduled on the
 * server thread with {@link MinecraftServer#execute(Runnable)} after a tick count
 * elapses (checked against the running task queue).</p>
 */
public final class FabricActionPlatform implements ActionPlatform {
    private final Supplier<MinecraftServer> serverSupplier;

    /**
     * Creates the platform adapter.
     *
     * @param serverSupplier supplier of the running server
     */
    public FabricActionPlatform(Supplier<MinecraftServer> serverSupplier) {
        this.serverSupplier = serverSupplier;
    }

    @Override
    public void dispatch(MagicSender sender, RunAs runAs, String commandLine) {
        MinecraftServer server = serverSupplier.get();
        CommandSourceStack source = unwrapSource(sender);
        if (server == null || source == null) {
            return;
        }
        CommandSourceStack effectiveSource = switch (runAs) {
            case CONSOLE -> server.createCommandSourceStack();
            case OP -> source.withMaximumPermission(PermissionSet.ALL_PERMISSIONS);
            case PLAYER -> source;
        };
        server.getCommands().performPrefixedCommand(effectiveSource, commandLine);
    }

    @Override
    public void runLater(long ticks, Runnable task) {
        MinecraftServer server = serverSupplier.get();
        if (server == null) {
            return;
        }
        if (ticks <= 0) {
            server.execute(task);
            return;
        }
        scheduleAfterTicks(server, ticks, task);
    }

    private void scheduleAfterTicks(MinecraftServer server, long ticks, Runnable task) {
        // Re-queue onto the server thread once per tick until the delay elapses.
        long[] remaining = { ticks };
        Runnable[] step = new Runnable[1];
        step[0] = () -> {
            if (--remaining[0] <= 0) {
                task.run();
            } else {
                server.execute(step[0]);
            }
        };
        server.execute(step[0]);
    }

    @Override
    public void message(MagicSender sender, Component message) {
        ServerPlayer player = unwrapPlayer(sender);
        if (player != null) {
            player.sendSystemMessage(FabricComponentSerializer.toNative(message));
        }
    }

    @Override
    public void broadcast(Component message) {
        MinecraftServer server = serverSupplier.get();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(FabricComponentSerializer.toNative(message), false);
        }
    }

    @Override
    public void sound(MagicSender sender, String sound, float volume, float pitch) {
        ServerPlayer player = unwrapPlayer(sender);
        if (player == null) {
            return;
        }
        Identifier key = Identifier.tryParse(sound);
        if (key == null) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(key);
        if (event == null) {
            return;
        }
        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event);
        player.connection.send(new ClientboundSoundPacket(
                holder, SoundSource.MASTER,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch, player.level().getRandom().nextLong()));
    }

    @Override
    public void title(MagicSender sender, Component title, Component subtitle,
                      int fadeInTicks, int stayTicks, int fadeOutTicks) {
        ServerPlayer player = unwrapPlayer(sender);
        if (player == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
        player.connection.send(new ClientboundSetSubtitleTextPacket(FabricComponentSerializer.toNative(subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(FabricComponentSerializer.toNative(title)));
    }

    @Override
    public void actionBar(MagicSender sender, Component message) {
        ServerPlayer player = unwrapPlayer(sender);
        if (player != null) {
            player.connection.send(new ClientboundSetActionBarTextPacket(FabricComponentSerializer.toNative(message)));
        }
    }

    private static CommandSourceStack unwrapSource(MagicSender sender) {
        Object handle = sender.handle();
        return handle instanceof CommandSourceStack source ? source : null;
    }

    private static ServerPlayer unwrapPlayer(MagicSender sender) {
        Object handle = sender.handle();
        if (handle instanceof CommandSourceStack source) {
            return source.getEntity() instanceof ServerPlayer player ? player : null;
        }
        return handle instanceof ServerPlayer player ? player : null;
    }
}
