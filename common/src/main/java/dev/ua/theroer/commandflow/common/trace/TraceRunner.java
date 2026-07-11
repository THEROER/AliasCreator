package dev.ua.theroer.commandflow.common.trace;

import java.util.HashMap;
import java.util.Map;

import dev.ua.theroer.commandflow.common.action.ActionContext;
import dev.ua.theroer.commandflow.common.action.ActionSequenceExecutor;
import dev.ua.theroer.commandflow.common.condition.ConditionChecker;
import dev.ua.theroer.commandflow.common.condition.ConditionEvaluator;
import dev.ua.theroer.commandflow.common.config.CommandEntry;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;

/**
 * Runs a command entry as a side-effect-free dry-run, recording every decision
 * into a {@link TraceRecorder}: cooldown, conditions, placeholder-resolved
 * target/actions, and cross-server routing.
 *
 * <p>Powers {@code /commandflow debug}: it wires the real placeholder resolution
 * and condition checks (world is checked for real; cost is shown but not charged)
 * against tracing action/dispatch stand-ins, so nothing actually executes.</p>
 */
public final class TraceRunner {
    private TraceRunner() {
    }

    /**
     * Dry-runs an entry and returns the recorded trace.
     *
     * @param alias alias name being traced
     * @param entry command configuration
     * @param sender invoking sender (for permissions/placeholders/world)
     * @param args resolved alias arguments (e.g. {sender}, {args})
     * @param checker real platform checker (world real; cost not charged)
     * @return recorder holding the ordered trace
     */
    public static TraceRecorder run(String alias, CommandEntry entry, MagicSender sender,
                                    Map<String, String> args, ConditionChecker checker) {
        TraceRecorder recorder = new TraceRecorder();
        Map<String, String> arguments = args != null ? new HashMap<>(args) : new HashMap<>();

        // Cooldown (informational; a dry-run never starts or blocks on it).
        long cooldown = entry.getCooldownSeconds();
        if (cooldown > 0) {
            recorder.record("cooldown", cooldown + "s per player", TraceStatus.INFO);
        }

        // Conditions.
        boolean pass = ConditionEvaluator.trace(entry.getConditions(), sender, arguments, checker, recorder);
        if (!pass) {
            recorder.record("result", "denied by a condition", TraceStatus.FAIL);
            return recorder;
        }

        // Actions (or legacy single target).
        TracingActionPlatform platform = new TracingActionPlatform(recorder);
        ActionContext context = new ActionContext(
                sender, platform, arguments, Component::text, new TracingRemoteDispatcher(recorder));

        if (entry.getActions() != null && !entry.getActions().isEmpty()) {
            ActionSequenceExecutor.run(entry.getActions(), context);
        } else {
            String resolved = context.resolve(entry.getTarget());
            recorder.record("target", "/" + resolved, TraceStatus.OK);
        }
        recorder.record("result", "would execute", TraceStatus.OK);
        return recorder;
    }
}
