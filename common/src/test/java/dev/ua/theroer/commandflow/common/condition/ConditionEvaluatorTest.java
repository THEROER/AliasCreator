package dev.ua.theroer.commandflow.common.condition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import dev.ua.theroer.commandflow.common.config.ConditionEntry;
import org.junit.jupiter.api.Test;

class ConditionEvaluatorTest {
    private static ConditionEntry cond(ConditionType type) {
        ConditionEntry c = new ConditionEntry();
        c.setType(type);
        return c;
    }

    @Test
    void emptyConditionsPass() {
        var result = ConditionEvaluator.evaluate(List.of(), null, Map.of(), ConditionChecker.PERMISSIVE);
        assertTrue(result.passed());
    }

    @Test
    void argEqualsMatchesCaseInsensitively() {
        ConditionEntry c = cond(ConditionType.ARG_EQUALS);
        c.setArg("mode");
        c.setValue("Fast");

        var pass = ConditionEvaluator.evaluate(List.of(c), null, Map.of("mode", "fast"), ConditionChecker.PERMISSIVE);
        assertTrue(pass.passed());

        var fail = ConditionEvaluator.evaluate(List.of(c), null, Map.of("mode", "slow"), ConditionChecker.PERMISSIVE);
        assertFalse(fail.passed());
        assertSame(c, fail.failed());
    }

    @Test
    void placeholderCompareUsesOperator() {
        ConditionEntry c = cond(ConditionType.PLACEHOLDER_COMPARE);
        c.setLeft("10");
        c.setOperator(CompareOperator.GREATER_EQUAL);
        c.setRight("5");

        var result = ConditionEvaluator.evaluate(List.of(c), null, Map.of(), ConditionChecker.PERMISSIVE);
        assertTrue(result.passed());
    }

    @Test
    void costIsChargedOnlyWhenOtherConditionsPass() {
        AtomicInteger charges = new AtomicInteger();
        ConditionChecker checker = new ConditionChecker() {
            @Override
            public boolean inWorld(dev.ua.theroer.magicutils.commands.MagicSender s, String w) {
                return false; // world condition will fail
            }

            @Override
            public boolean chargeCost(dev.ua.theroer.magicutils.commands.MagicSender s, double amount) {
                charges.incrementAndGet();
                return true;
            }
        };

        ConditionEntry cost = cond(ConditionType.COST);
        cost.setAmount(100);
        ConditionEntry world = cond(ConditionType.WORLD);
        world.setValue("survival");

        // World fails, so cost must NOT be charged.
        var result = ConditionEvaluator.evaluate(List.of(cost, world), null, Map.of(), checker);
        assertFalse(result.passed());
        assertSame(world, result.failed());
        assertTrue(charges.get() == 0, "cost must not be charged when a gating condition fails");
    }

    @Test
    void costChargedWhenEverythingElsePasses() {
        AtomicInteger charges = new AtomicInteger();
        ConditionChecker checker = new ConditionChecker() {
            @Override
            public boolean inWorld(dev.ua.theroer.magicutils.commands.MagicSender s, String w) {
                return true;
            }

            @Override
            public boolean chargeCost(dev.ua.theroer.magicutils.commands.MagicSender s, double amount) {
                charges.incrementAndGet();
                return true;
            }
        };
        ConditionEntry cost = cond(ConditionType.COST);
        cost.setAmount(100);

        var result = ConditionEvaluator.evaluate(List.of(cost), null, Map.of(), checker);
        assertTrue(result.passed());
        assertTrue(charges.get() == 1);
    }

    @Test
    void operatorParsesSymbolsAndNames() {
        assertSame(CompareOperator.GREATER_EQUAL, CompareOperator.from(">="));
        assertSame(CompareOperator.GREATER_EQUAL, CompareOperator.from("GREATER_EQUAL"));
        assertSame(CompareOperator.NOT_EQUALS, CompareOperator.from("!="));
        assertTrue(CompareOperator.LESS.test("3", "10"), "numeric compare");
        assertFalse(CompareOperator.LESS.test("abc", "abc"));
        assertTrue(CompareOperator.CONTAINS.test("hello world", "world"));
    }
}
