package dev.ua.theroer.commandflow.bukkit;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.title.Title;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit/Paper implementation of {@link ActionPlatform}.
 *
 * <p>Commands run through {@link Bukkit#dispatchCommand}. Player-facing effects
 * use Paper's Adventure support on {@link Player}; {@code op} runs temporarily
 * grant op for the single dispatch. Delays use the Bukkit scheduler.</p>
 */
public final class BukkitActionPlatform implements ActionPlatform {
    private static final long TICK_MILLIS = 50L;

    private final Plugin plugin;

    /**
     * Creates the platform adapter.
     *
     * @param plugin owning plugin (for the scheduler)
     */
    public BukkitActionPlatform(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void dispatch(MagicSender sender, RunAs runAs, String commandLine) {
        switch (runAs) {
            case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
            case OP -> dispatchAsOp(sender, commandLine);
            case PLAYER -> {
                CommandSender bukkitSender = unwrapSender(sender);
                if (bukkitSender != null) {
                    Bukkit.dispatchCommand(bukkitSender, commandLine);
                }
            }
        }
    }

    private void dispatchAsOp(MagicSender sender, String commandLine) {
        CommandSender bukkitSender = unwrapSender(sender);
        if (bukkitSender == null) {
            return;
        }
        if (!(bukkitSender instanceof Player player) || player.isOp()) {
            Bukkit.dispatchCommand(bukkitSender, commandLine);
            return;
        }
        try {
            player.setOp(true);
            Bukkit.dispatchCommand(player, commandLine);
        } finally {
            player.setOp(false);
        }
    }

    @Override
    public void runLater(long ticks, Runnable task) {
        if (ticks <= 0) {
            task.run();
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    @Override
    public void message(MagicSender sender, Component message) {
        CommandSender bukkitSender = unwrapSender(sender);
        if (bukkitSender != null) {
            bukkitSender.sendMessage(message);
        }
    }

    @Override
    public void broadcast(Component message) {
        Bukkit.broadcast(message);
    }

    @Override
    public void sound(MagicSender sender, String sound, float volume, float pitch) {
        Player player = unwrapPlayer(sender);
        if (player != null) {
            player.playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, volume, pitch));
        }
    }

    @Override
    public void title(MagicSender sender, Component title, Component subtitle,
                      int fadeInTicks, int stayTicks, int fadeOutTicks) {
        Player player = unwrapPlayer(sender);
        if (player == null) {
            return;
        }
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * TICK_MILLIS),
                Duration.ofMillis(stayTicks * TICK_MILLIS),
                Duration.ofMillis(fadeOutTicks * TICK_MILLIS));
        player.showTitle(Title.title(title, subtitle, times));
    }

    @Override
    public void actionBar(MagicSender sender, Component message) {
        Player player = unwrapPlayer(sender);
        if (player != null) {
            player.sendActionBar(message);
        }
    }

    private static CommandSender unwrapSender(MagicSender sender) {
        Object handle = sender.handle();
        return handle instanceof CommandSender commandSender ? commandSender : null;
    }

    private static Player unwrapPlayer(MagicSender sender) {
        Object handle = sender.handle();
        return handle instanceof Player player ? player : null;
    }
}
