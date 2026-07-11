package dev.ua.theroer.commandflow.common.trace;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import dev.ua.theroer.commandflow.common.action.ActionType;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.commandflow.common.condition.CompareOperator;
import dev.ua.theroer.commandflow.common.condition.ConditionChecker;
import dev.ua.theroer.commandflow.common.condition.ConditionType;
import dev.ua.theroer.commandflow.common.config.ActionEntry;
import dev.ua.theroer.commandflow.common.config.AliasEntry;
import dev.ua.theroer.commandflow.common.config.ConditionEntry;
import org.junit.jupiter.api.Test;

class TraceRunnerTest {
    private static ActionEntry action(ActionType type) {
        ActionEntry e = new ActionEntry();
        e.setType(type);
        return e;
    }

    private static ConditionEntry condition(ConditionType type) {
        ConditionEntry c = new ConditionEntry();
        c.setType(type);
        return c;
    }

    private static boolean anyDetailContains(List<TraceStep> steps, String needle) {
        return steps.stream().anyMatch(s -> s.detail() != null && s.detail().contains(needle));
    }

    @Test
    void tracesConditionsAndActionsWithoutSideEffects() {
        AliasEntry entry = new AliasEntry();
        entry.getAliases().add("kit");

        ConditionEntry cmp = condition(ConditionType.PLACEHOLDER_COMPARE);
        cmp.setLeft("10");
        cmp.setOperator(CompareOperator.GREATER_EQUAL);
        cmp.setRight("5");
        entry.getConditions().add(cmp);

        ActionEntry run = action(ActionType.RUN);
        run.setCommand("give {sender} diamond");
        run.setAs(RunAs.CONSOLE);
        entry.getActions().add(run);

        TraceRecorder recorder = TraceRunner.run("kit", entry, null, Map.of("sender", "Steve"),
                ConditionChecker.PERMISSIVE);
        List<TraceStep> steps = recorder.steps();

        // Condition recorded and passed.
        assertTrue(steps.stream().anyMatch(s ->
                s.label().contains("PLACEHOLDER_COMPARE") && s.status() == TraceStatus.OK));
        // Action recorded with placeholder substituted, and the sequence "would execute".
        assertTrue(anyDetailContains(steps, "give Steve diamond"), "placeholder should resolve in trace");
        assertTrue(steps.stream().anyMatch(s -> "result".equals(s.label()) && s.status() == TraceStatus.OK));
    }

    @Test
    void failingConditionStopsTraceWithFailResult() {
        AliasEntry entry = new AliasEntry();
        entry.getAliases().add("vip");
        ConditionEntry cmp = condition(ConditionType.PLACEHOLDER_COMPARE);
        cmp.setLeft("1");
        cmp.setOperator(CompareOperator.GREATER);
        cmp.setRight("100");
        entry.getConditions().add(cmp);

        TraceRecorder recorder = TraceRunner.run("vip", entry, null, Map.of(), ConditionChecker.PERMISSIVE);
        List<TraceStep> steps = recorder.steps();

        assertTrue(steps.stream().anyMatch(s -> s.status() == TraceStatus.FAIL));
        assertTrue(steps.stream().anyMatch(s -> "result".equals(s.label()) && s.status() == TraceStatus.FAIL));
    }

    @Test
    void queryActionIsTracedAsRoute() {
        AliasEntry entry = new AliasEntry();
        entry.getAliases().add("bal");
        ActionEntry query = action(ActionType.QUERY);
        query.setServer("economy");
        query.setPlaceholder("%balance%");
        query.setInto("bal");
        entry.getActions().add(query);
        ActionEntry msg = action(ActionType.MESSAGE);
        msg.setText("balance is {bal}");
        entry.getActions().add(msg);

        TraceRecorder recorder = TraceRunner.run("bal", entry, null, Map.of(), ConditionChecker.PERMISSIVE);
        List<TraceStep> steps = recorder.steps();

        assertTrue(steps.stream().anyMatch(s -> s.label().contains("route QUERY")), "query should be traced as a route");
        // The dry-run query marker feeds back into the following message.
        assertTrue(anyDetailContains(steps, "<query result>"), "query result marker should appear in the message");
    }

    @Test
    void renderProducesHeaderAndLines() {
        AliasEntry entry = new AliasEntry();
        entry.getAliases().add("spawn");
        entry.setTarget("spawn");

        TraceRecorder recorder = TraceRunner.run("spawn", entry, null, Map.of(), ConditionChecker.PERMISSIVE);
        List<String> lines = recorder.render("spawn");

        assertFalse(lines.isEmpty());
        assertTrue(lines.get(0).contains("debug"), "first line is the header");
    }
}
