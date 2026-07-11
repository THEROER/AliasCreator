package dev.ua.theroer.commandflow.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.condition.ConditionChecker;
import dev.ua.theroer.commandflow.common.condition.ConditionEvaluator;
import dev.ua.theroer.commandflow.common.config.CommandEntry;
import dev.ua.theroer.commandflow.common.cooldown.CooldownStore;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.placeholders.MagicPlaceholders;
import net.kyori.adventure.text.Component;

/**
 * Shared pre-execution gate applied on every platform before a command's target
 * or action sequence runs: it evaluates conditions and the per-player cooldown,
 * sends the configured deny / cooldown message on failure, and starts the
 * cooldown on success.
 *
 * <p>Centralising this keeps the bukkit and fabric registrars from duplicating
 * the check, and keeps deny messaging consistent.</p>
 */
public final class CommandGate {
    private static final String DEFAULT_DENY = "<red>You cannot use this command.";
    private static final String DEFAULT_COOLDOWN = "<red>You must wait {cooldown-remaining}s.";

    private final CooldownStore cooldowns;
    private final ConditionChecker checker;
    private final Function<String, Component> textParser;

    /**
     * Creates a gate.
     *
     * @param cooldowns cooldown store
     * @param checker platform condition checker (world/economy)
     * @param textParser MiniMessage parser for deny/cooldown messages
     */
    public CommandGate(CooldownStore cooldowns, ConditionChecker checker,
                       Function<String, Component> textParser) {
        this.cooldowns = cooldowns != null ? cooldowns : new CooldownStore();
        this.checker = checker != null ? checker : ConditionChecker.PERMISSIVE;
        this.textParser = textParser != null ? textParser : Component::text;
    }

    /**
     * Checks whether a command may run for the sender. On denial, sends the
     * appropriate message via the platform and returns false. On success, starts
     * the cooldown and returns true.
     *
     * @param commandKey stable key for cooldown tracking (the alias name)
     * @param entry command configuration (conditions, cooldown, messages)
     * @param sender invoking sender
     * @param args resolved alias arguments (for ARG_EQUALS / placeholders)
     * @param platform platform used to deliver deny messages
     * @return true when the command may proceed
     */
    public boolean check(String commandKey, CommandEntry entry, MagicSender sender,
                         Map<String, String> args, ActionPlatform platform) {
        Map<String, String> arguments = args != null ? args : Map.of();
        UUID playerId = sender != null ? sender.id() : null;

        long cooldownSeconds = entry.getCooldownSeconds();
        boolean bypass = !entry.getCooldownBypass().isBlank()
                && sender != null && sender.hasPermission(entry.getCooldownBypass());
        if (cooldownSeconds > 0 && !bypass && playerId != null) {
            long remaining = cooldowns.remainingSeconds(commandKey, playerId, cooldownSeconds);
            if (remaining > 0) {
                sendCooldown(entry, sender, arguments, platform, remaining);
                return false;
            }
        }

        ConditionEvaluator.Result result =
                ConditionEvaluator.evaluate(entry.getConditions(), sender, arguments, checker);
        if (!result.passed()) {
            sendDeny(entry, sender, arguments, platform);
            return false;
        }

        if (cooldownSeconds > 0 && !bypass && playerId != null) {
            cooldowns.markUsed(commandKey, playerId);
        }
        return true;
    }

    /**
     * @return the underlying cooldown store (e.g. to clear on reload)
     */
    public CooldownStore cooldowns() {
        return cooldowns;
    }

    private void sendDeny(CommandEntry entry, MagicSender sender, Map<String, String> args, ActionPlatform platform) {
        String raw = !entry.getDenyMessage().isBlank() ? entry.getDenyMessage() : DEFAULT_DENY;
        platform.message(sender, render(sender, raw, args));
    }

    private void sendCooldown(CommandEntry entry, MagicSender sender, Map<String, String> args,
                              ActionPlatform platform, long remaining) {
        String raw = !entry.getCooldownMessage().isBlank() ? entry.getCooldownMessage() : DEFAULT_COOLDOWN;
        Map<String, String> withRemaining = new HashMap<>(args);
        withRemaining.put("cooldown-remaining", String.valueOf(remaining));
        platform.message(sender, render(sender, raw, withRemaining));
    }

    private Component render(MagicSender sender, String raw, Map<String, String> args) {
        var audience = sender != null ? sender.audience() : null;
        return textParser.apply(MagicPlaceholders.render(audience, raw, args));
    }
}
