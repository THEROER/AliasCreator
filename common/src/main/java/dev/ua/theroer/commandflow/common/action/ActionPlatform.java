package dev.ua.theroer.commandflow.common.action;

import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;

/**
 * Platform-specific effects an action sequence performs.
 *
 * <p>The action engine in {@code common} is platform-agnostic: it parses and
 * orchestrates the sequence, and delegates every side effect to this interface,
 * implemented per platform (Bukkit, Fabric). This mirrors how the alias
 * suggestion providers are split (interface in common, implementation per
 * platform).</p>
 */
public interface ActionPlatform {
    /**
     * Runs a command on this server as the given identity.
     *
     * @param sender the invoking player/sender
     * @param runAs identity to run as (player, console, or op)
     * @param commandLine command line to run (no leading slash)
     */
    void dispatch(MagicSender sender, RunAs runAs, String commandLine);

    /**
     * Schedules a task to run later on the main thread after a tick delay.
     *
     * <p>Used to implement {@link ActionType#DELAY} without blocking. On
     * platforms without a native tick scheduler this may approximate with a
     * timer; a non-positive delay runs the task immediately.</p>
     *
     * @param ticks delay in ticks (20 ticks = 1 second)
     * @param task work to run after the delay
     */
    void runLater(long ticks, Runnable task);

    /**
     * Sends a message component to the invoking player.
     *
     * @param sender the invoking player/sender
     * @param message message component
     */
    void message(MagicSender sender, Component message);

    /**
     * Broadcasts a message component to everyone on the server.
     *
     * @param message message component
     */
    void broadcast(Component message);

    /**
     * Plays a sound to the invoking player.
     *
     * @param sender the invoking player/sender
     * @param sound sound key (e.g. {@code entity.player.levelup})
     * @param volume volume
     * @param pitch pitch
     */
    void sound(MagicSender sender, String sound, float volume, float pitch);

    /**
     * Shows a title/subtitle to the invoking player.
     *
     * @param sender the invoking player/sender
     * @param title title component
     * @param subtitle subtitle component
     * @param fadeInTicks fade-in ticks
     * @param stayTicks stay ticks
     * @param fadeOutTicks fade-out ticks
     */
    void title(MagicSender sender, Component title, Component subtitle,
               int fadeInTicks, int stayTicks, int fadeOutTicks);

    /**
     * Shows an action bar message to the invoking player.
     *
     * @param sender the invoking player/sender
     * @param message action bar component
     */
    void actionBar(MagicSender sender, Component message);
}
