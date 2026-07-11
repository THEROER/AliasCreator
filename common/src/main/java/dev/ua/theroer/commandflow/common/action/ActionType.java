package dev.ua.theroer.commandflow.common.action;

/**
 * Kind of a single {@link dev.ua.theroer.commandflow.common.config.ActionEntry}
 * step in a command's action sequence.
 *
 * <p>Serialized directly as a config enum value by MagicUtils' {@code EnumAdapter}
 * (matched case-insensitively against {@link #name()}), so no manual parsing is
 * needed.</p>
 */
public enum ActionType {
    /** Run a command (as player, console, or op) on this server. */
    RUN,
    /** Send a message to the invoking player. */
    MESSAGE,
    /** Broadcast a message to everyone on the server. */
    BROADCAST,
    /** Pause the sequence for a number of ticks before the next step. */
    DELAY,
    /** Play a sound to the invoking player. */
    SOUND,
    /** Show a title/subtitle to the invoking player. */
    TITLE,
    /** Show an action bar message to the invoking player. */
    ACTIONBAR,
    /**
     * Query a placeholder's value from another server, store it in an inline
     * variable, and continue the sequence once the reply arrives. The
     * cross-server killer feature.
     */
    QUERY
}
