package dev.ua.theroer.commandflow.common.action;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.placeholders.MagicPlaceholders;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Per-invocation context for running an action sequence.
 *
 * <p>Bundles the invoking {@link MagicSender}, the {@link ActionPlatform} that
 * performs effects, the inline argument values ({@code {sender}}, {@code {args}},
 * per-template argument names), and a text parser turning resolved strings into
 * {@link Component}s (MiniMessage on platforms that ship it).</p>
 *
 * <p>Placeholder resolution delegates to {@link MagicPlaceholders#render}, so a
 * field may reference registered server placeholders (including PAPI / PB4 /
 * MiniPlaceholders through MagicUtils' bridges) alongside the inline arguments —
 * one resolver for every engine, rather than a hand-rolled substitution.</p>
 */
public final class ActionContext {
    private final MagicSender sender;
    private final ActionPlatform platform;
    private final Map<String, String> inline;
    private final Function<String, Component> textParser;
    private final RemoteDispatcher remote;

    /**
     * Creates a local-only action context (no cross-server dispatch).
     *
     * @param sender invoking sender
     * @param platform platform effect adapter
     * @param inline inline argument values (name → value), applied by MagicPlaceholders
     * @param textParser turns a resolved string into a display component (MiniMessage)
     */
    public ActionContext(MagicSender sender, ActionPlatform platform,
                         Map<String, String> inline, Function<String, Component> textParser) {
        this(sender, platform, inline, textParser, RemoteDispatcher.DISABLED);
    }

    /**
     * Creates an action context with cross-server dispatch.
     *
     * @param sender invoking sender
     * @param platform platform effect adapter
     * @param inline inline argument values (name → value); copied into a mutable map
     *               so QUERY actions can store their results
     * @param textParser turns a resolved string into a display component (MiniMessage)
     * @param remote cross-server dispatcher (use {@link RemoteDispatcher#DISABLED} when off)
     */
    public ActionContext(MagicSender sender, ActionPlatform platform, Map<String, String> inline,
                         Function<String, Component> textParser, RemoteDispatcher remote) {
        this.sender = sender;
        this.platform = platform;
        this.inline = new HashMap<>(inline != null ? inline : Map.of());
        this.textParser = textParser != null ? textParser : Component::text;
        this.remote = remote != null ? remote : RemoteDispatcher.DISABLED;
    }

    /**
     * @return invoking sender
     */
    public MagicSender sender() {
        return sender;
    }

    /**
     * @return platform effect adapter
     */
    public ActionPlatform platform() {
        return platform;
    }

    /**
     * @return cross-server dispatcher (never null; {@link RemoteDispatcher#DISABLED} when off)
     */
    public RemoteDispatcher remote() {
        return remote;
    }

    /**
     * Stores an inline variable so later actions can reference it as {@code {name}}.
     *
     * @param name variable name
     * @param value value (null stored as empty)
     */
    public void setVariable(String name, String value) {
        if (name != null && !name.isBlank()) {
            inline.put(name, value != null ? value : "");
        }
    }

    /**
     * Resolves inline arguments and registered placeholders in a raw field value.
     *
     * @param raw raw string with placeholders
     * @return resolved string (never null)
     */
    public String resolve(String raw) {
        if (raw == null) {
            return "";
        }
        String result = MagicPlaceholders.render(audience(), raw, inline);
        return result != null ? result : "";
    }

    /**
     * Resolves placeholders then parses the result into a component.
     *
     * @param raw raw string with placeholders
     * @return display component
     */
    public Component render(String raw) {
        return textParser.apply(resolve(raw));
    }

    private @Nullable dev.ua.theroer.magicutils.platform.Audience audience() {
        return sender != null ? sender.audience() : null;
    }
}
