package dev.ua.theroer.commandflow.common.condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.ua.theroer.commandflow.common.config.ConditionEntry;
import dev.ua.theroer.magicutils.commands.MagicSender;
import dev.ua.theroer.magicutils.placeholders.MagicPlaceholders;

/**
 * Evaluates a command's condition list as an all-must-pass gate.
 *
 * <p>Non-charging conditions (permission, world, argument, placeholder-compare)
 * are checked first; only if every one passes are {@link ConditionType#COST}
 * conditions charged, so a player is never charged for a command that a later
 * condition would have denied. Returns the first failure, or success.</p>
 */
public final class ConditionEvaluator {
    private ConditionEvaluator() {
    }

    /**
     * Outcome of evaluating a condition list.
     *
     * @param passed whether all conditions passed
     * @param failed the condition that failed (null when passed)
     */
    public record Result(boolean passed, ConditionEntry failed) {
        static Result ok() {
            return new Result(true, null);
        }

        static Result fail(ConditionEntry entry) {
            return new Result(false, entry);
        }
    }

    /**
     * Evaluates all conditions.
     *
     * @param conditions condition list (null/empty always passes)
     * @param sender invoking sender
     * @param args resolved alias arguments (name → value) for ARG_EQUALS
     * @param checker platform world/economy checker
     * @return evaluation result
     */
    public static Result evaluate(List<ConditionEntry> conditions, MagicSender sender,
                                  Map<String, String> args, ConditionChecker checker) {
        if (conditions == null || conditions.isEmpty()) {
            return Result.ok();
        }
        ConditionChecker platform = checker != null ? checker : ConditionChecker.PERMISSIVE;
        Map<String, String> arguments = args != null ? args : Map.of();

        List<ConditionEntry> costs = new ArrayList<>();
        for (ConditionEntry condition : conditions) {
            if (condition == null) {
                continue;
            }
            ConditionType type = condition.getType() != null ? condition.getType() : ConditionType.PERMISSION;
            if (type == ConditionType.COST) {
                costs.add(condition);
                continue;
            }
            if (!checkNonCharging(type, condition, sender, arguments, platform)) {
                return Result.fail(condition);
            }
        }

        // All gating conditions passed; now charge any costs.
        for (ConditionEntry cost : costs) {
            if (!platform.chargeCost(sender, cost.getAmount())) {
                return Result.fail(cost);
            }
        }
        return Result.ok();
    }

    /**
     * Evaluates conditions for a dry-run trace: records each condition's outcome
     * without stopping at the first failure and without charging any cost.
     *
     * @param conditions condition list
     * @param sender invoking sender
     * @param args resolved alias arguments
     * @param checker platform world checker (cost is not charged in a trace)
     * @param recorder trace recorder
     * @return true when every condition would pass
     */
    public static boolean trace(List<ConditionEntry> conditions, MagicSender sender,
                                Map<String, String> args, ConditionChecker checker,
                                dev.ua.theroer.commandflow.common.trace.TraceRecorder recorder) {
        if (conditions == null || conditions.isEmpty()) {
            recorder.record("conditions", "none", dev.ua.theroer.commandflow.common.trace.TraceStatus.SKIP);
            return true;
        }
        ConditionChecker platform = checker != null ? checker : ConditionChecker.PERMISSIVE;
        Map<String, String> arguments = args != null ? args : Map.of();
        boolean allPass = true;
        for (ConditionEntry condition : conditions) {
            if (condition == null) {
                continue;
            }
            ConditionType type = condition.getType() != null ? condition.getType() : ConditionType.PERMISSION;
            if (type == ConditionType.COST) {
                recorder.record("condition COST", "would charge " + condition.getAmount() + " (not charged in dry-run)",
                        dev.ua.theroer.commandflow.common.trace.TraceStatus.INFO);
                continue;
            }
            boolean pass = checkNonCharging(type, condition, sender, arguments, platform);
            allPass &= pass;
            recorder.record("condition " + type, describe(type, condition, arguments),
                    pass ? dev.ua.theroer.commandflow.common.trace.TraceStatus.OK
                            : dev.ua.theroer.commandflow.common.trace.TraceStatus.FAIL);
        }
        return allPass;
    }

    private static String describe(ConditionType type, ConditionEntry condition, Map<String, String> args) {
        return switch (type) {
            case PERMISSION -> condition.getValue();
            case WORLD -> "world = " + condition.getValue();
            case ARG_EQUALS -> condition.getArg() + " == " + condition.getValue()
                    + " (actual: " + args.getOrDefault(condition.getArg(), "") + ")";
            case PLACEHOLDER_COMPARE -> condition.getLeft() + " " + condition.getOperator() + " " + condition.getRight();
            case COST -> String.valueOf(condition.getAmount());
        };
    }

    private static boolean checkNonCharging(ConditionType type, ConditionEntry condition, MagicSender sender,
                                            Map<String, String> args, ConditionChecker platform) {
        return switch (type) {
            case PERMISSION -> {
                String node = condition.getValue();
                yield node == null || node.isBlank() || (sender != null && sender.hasPermission(node));
            }
            case WORLD -> platform.inWorld(sender, condition.getValue());
            case ARG_EQUALS -> {
                String actual = args.get(condition.getArg());
                yield actual != null && actual.equalsIgnoreCase(condition.getValue());
            }
            case PLACEHOLDER_COMPARE -> {
                var audience = sender != null ? sender.audience() : null;
                String left = MagicPlaceholders.render(audience, condition.getLeft(), args);
                String right = MagicPlaceholders.render(audience, condition.getRight(), args);
                CompareOperator op = condition.getOperator() != null ? condition.getOperator() : CompareOperator.EQUALS;
                yield op.test(left, right);
            }
            case COST -> true; // charged separately
        };
    }
}
