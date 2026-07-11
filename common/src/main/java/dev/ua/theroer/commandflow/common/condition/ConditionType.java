package dev.ua.theroer.commandflow.common.condition;

/**
 * Kind of a single {@link dev.ua.theroer.commandflow.common.config.ConditionEntry}
 * gating a command.
 *
 * <p>Serialized directly as a config enum value by MagicUtils' {@code EnumAdapter}.</p>
 */
public enum ConditionType {
    /** Requires the sender to hold a permission node. */
    PERMISSION,
    /** Requires the player to be in a named world. */
    WORLD,
    /** Requires a specific alias argument to equal a value. */
    ARG_EQUALS,
    /** Compares two placeholder-resolved strings with an operator. */
    PLACEHOLDER_COMPARE,
    /** Requires the player to afford (and pay) an economy cost. Bukkit + Vault only. */
    COST
}
