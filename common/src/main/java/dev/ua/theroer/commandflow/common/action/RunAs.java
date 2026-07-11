package dev.ua.theroer.commandflow.common.action;

/**
 * Identity a {@link ActionType#RUN} action executes as.
 *
 * <p>Serialized directly as a config enum value by MagicUtils' {@code EnumAdapter}.</p>
 */
public enum RunAs {
    /** Run as the invoking player, with their permissions. */
    PLAYER,
    /** Run from the server console. */
    CONSOLE,
    /** Run as the invoking player but with a temporary permission bypass (op). */
    OP
}
