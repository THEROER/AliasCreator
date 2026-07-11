package dev.ua.theroer.commandflow.common.config;

import dev.ua.theroer.commandflow.common.action.ActionType;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.magicutils.config.annotations.Comment;
import dev.ua.theroer.magicutils.config.annotations.ConfigSerializable;
import dev.ua.theroer.magicutils.config.annotations.ConfigValue;
import dev.ua.theroer.magicutils.config.annotations.DefaultValue;
import lombok.Getter;
import lombok.Setter;

/**
 * One step in a command's action sequence.
 *
 * <p>A single flat object holds the fields for every {@link
 * dev.ua.theroer.commandflow.common.action.ActionType action type}; only the
 * fields relevant to {@link #type} are read at execution time. Keeping the shape
 * flat makes the YAML easy to read and write by hand.</p>
 *
 * <pre>
 * actions:
 *   - type: run
 *     as: console
 *     command: "effect give {sender} regeneration 5"
 *   - type: message
 *     text: "&lt;green&gt;You are healed!"
 *   - type: sound
 *     sound: entity.player.levelup
 *   - type: delay
 *     ticks: 20
 * </pre>
 */
@Getter
@Setter
@ConfigSerializable
public class ActionEntry {
    @ConfigValue("type")
    @Comment("Action type: RUN | MESSAGE | BROADCAST | DELAY | SOUND | TITLE | ACTIONBAR")
    private ActionType type = ActionType.RUN;

    @ConfigValue("command")
    @Comment("[run] Command line to execute (no leading slash), placeholders allowed")
    private String command = "";

    @ConfigValue("as")
    @Comment("[run] Who runs the command: PLAYER | CONSOLE | OP")
    private RunAs as = RunAs.PLAYER;

    @ConfigValue("server")
    @DefaultValue("")
    @Comment("[run/query] Where to run. Empty = this server; a backend name; "
            + "'*' = all backends; '@player' = the backend hosting the player.")
    private String server = "";

    @ConfigValue("placeholder")
    @DefaultValue("")
    @Comment("[query] Placeholder string to resolve on the target server")
    private String placeholder = "";

    @ConfigValue("into")
    @DefaultValue("")
    @Comment("[query] Inline variable name to store the result in (used as {name} later)")
    private String into = "";

    @ConfigValue("timeout-ticks")
    @DefaultValue("40")
    @Comment("[query] Ticks to wait for the reply before stopping the sequence (20 = 1s)")
    private long timeoutTicks = 40;

    @ConfigValue("text")
    @DefaultValue("")
    @Comment("[message/broadcast/actionbar] MiniMessage text, placeholders allowed")
    private String text = "";

    @ConfigValue("ticks")
    @DefaultValue("0")
    @Comment("[delay] Ticks to wait before the next action (20 ticks = 1 second)")
    private long ticks = 0;

    @ConfigValue("sound")
    @DefaultValue("")
    @Comment("[sound] Sound key, e.g. entity.player.levelup")
    private String sound = "";

    @ConfigValue("volume")
    @DefaultValue("1.0")
    @Comment("[sound] Volume")
    private float volume = 1.0f;

    @ConfigValue("pitch")
    @DefaultValue("1.0")
    @Comment("[sound] Pitch")
    private float pitch = 1.0f;

    @ConfigValue("title")
    @DefaultValue("")
    @Comment("[title] Title line (MiniMessage), placeholders allowed")
    private String title = "";

    @ConfigValue("subtitle")
    @DefaultValue("")
    @Comment("[title] Subtitle line (MiniMessage), placeholders allowed")
    private String subtitle = "";

    @ConfigValue("fade-in")
    @DefaultValue("10")
    @Comment("[title] Fade-in ticks")
    private int fadeIn = 10;

    @ConfigValue("stay")
    @DefaultValue("40")
    @Comment("[title] Stay ticks")
    private int stay = 40;

    @ConfigValue("fade-out")
    @DefaultValue("10")
    @Comment("[title] Fade-out ticks")
    private int fadeOut = 10;
}
