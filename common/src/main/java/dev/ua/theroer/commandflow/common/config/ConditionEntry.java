package dev.ua.theroer.commandflow.common.config;

import dev.ua.theroer.commandflow.common.condition.CompareOperator;
import dev.ua.theroer.commandflow.common.condition.ConditionType;
import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import lombok.Getter;
import lombok.Setter;

/**
 * One condition gating a command. A command runs only when all its conditions
 * pass; otherwise the command's deny message is shown.
 *
 * <p>A flat object holds the fields for every {@link ConditionType}; only the
 * fields relevant to {@link #type} are read.</p>
 *
 * <pre>
 * conditions:
 *   - type: PERMISSION
 *     value: cf.kit.vip
 *   - type: WORLD
 *     value: survival
 *   - type: PLACEHOLDER_COMPARE
 *     left: "%player_level%"
 *     operator: "&gt;="
 *     right: "10"
 *   - type: COST
 *     amount: 100.0
 * </pre>
 */
@Getter
@Setter
@ConfigSerializable
public class ConditionEntry {
    @ConfigValue("type")
    @Comment("Condition type: PERMISSION | WORLD | ARG_EQUALS | PLACEHOLDER_COMPARE | COST")
    private ConditionType type = ConditionType.PERMISSION;

    @ConfigValue("value")
    @Comment("[permission] node | [world] world name | [arg-equals] expected value")
    private String value = "";

    @ConfigValue("arg")
    @Comment("[arg-equals] argument name to compare")
    private String arg = "";

    @ConfigValue("left")
    @Comment("[placeholder-compare] left side (placeholders allowed)")
    private String left = "";

    @ConfigValue("operator")
    @Comment("[placeholder-compare] EQUALS | NOT_EQUALS | GREATER | GREATER_EQUAL | LESS | LESS_EQUAL | CONTAINS (or ==, >, >=, <, <=, ~)")
    private CompareOperator operator = CompareOperator.EQUALS;

    @ConfigValue("right")
    @Comment("[placeholder-compare] right side (placeholders allowed)")
    private String right = "";

    @ConfigValue("amount")
    @Comment("[cost] economy amount to charge (Bukkit + Vault only)")
    private double amount = 0.0;
}
