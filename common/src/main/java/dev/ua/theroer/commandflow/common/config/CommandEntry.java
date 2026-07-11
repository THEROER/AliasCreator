package dev.ua.theroer.commandflow.common.config;

import java.util.ArrayList;
import java.util.List;

import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * Fields shared by every kind of CommandFlow command entry (simple aliases and
 * template aliases): a target command, an optional action sequence, gating
 * conditions, cooldown settings, and a permission.
 *
 * <p>The config serializer walks superclass fields, so subclasses inherit these
 * config values directly. Keeping them here avoids duplicating the block across
 * entry types.</p>
 */
@Getter
@Setter
public abstract class CommandEntry {
    @ConfigValue("target")
    @Comment("Target command to execute (used when no action sequence is set)")
    private String target = "";

    @ConfigValue("permission")
    @DefaultValue("")
    @Comment("Permission required to use the command (empty = no permission)")
    private String permission = "";

    @ConfigValue("conditions")
    @Comment("Conditions that must all pass before the command runs (empty = always)")
    private List<ConditionEntry> conditions = new ArrayList<>();

    @ConfigValue("actions")
    @Comment("Optional action sequence run when the command is used. When empty, the "
            + "single 'target' command runs instead.")
    private List<ActionEntry> actions = new ArrayList<>();

    @ConfigValue("cooldown-seconds")
    @DefaultValue("0")
    @Comment("Per-player cooldown in seconds (0 = none)")
    private long cooldownSeconds = 0;

    @ConfigValue("cooldown-bypass")
    @DefaultValue("")
    @Comment("Permission that bypasses the cooldown (empty = nobody bypasses)")
    private String cooldownBypass = "";

    @ConfigValue("cooldown-message")
    @DefaultValue("")
    @Comment("Message shown while on cooldown. Placeholder {cooldown-remaining} = seconds left.")
    private String cooldownMessage = "";

    @ConfigValue("deny-message")
    @DefaultValue("")
    @Comment("Message shown when a condition fails (empty = a default message)")
    private String denyMessage = "";
}
