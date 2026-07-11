package dev.ua.theroer.commandflow.common.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.ua.theroer.commandflow.common.config.ActionEntry;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class ActionSequenceExecutorTest {
    /** Records every effect the engine requests, in order. */
    private static final class RecordingPlatform implements ActionPlatform {
        final List<String> log = new ArrayList<>();

        @Override
        public void dispatch(dev.ua.theroer.magicutils.commands.MagicSender sender, RunAs runAs, String commandLine) {
            log.add("run[" + runAs + "]:" + commandLine);
        }

        @Override
        public void runLater(long ticks, Runnable task) {
            log.add("delay:" + ticks);
            task.run(); // execute synchronously so the sequence completes in-test
        }

        @Override
        public void message(dev.ua.theroer.magicutils.commands.MagicSender sender, Component message) {
            log.add("message:" + plain(message));
        }

        @Override
        public void broadcast(Component message) {
            log.add("broadcast:" + plain(message));
        }

        @Override
        public void sound(dev.ua.theroer.magicutils.commands.MagicSender sender, String sound, float volume, float pitch) {
            log.add("sound:" + sound);
        }

        @Override
        public void title(dev.ua.theroer.magicutils.commands.MagicSender sender, Component title, Component subtitle,
                          int fadeInTicks, int stayTicks, int fadeOutTicks) {
            log.add("title:" + plain(title) + "/" + plain(subtitle));
        }

        @Override
        public void actionBar(dev.ua.theroer.magicutils.commands.MagicSender sender, Component message) {
            log.add("actionbar:" + plain(message));
        }

        private static String plain(Component c) {
            return c instanceof net.kyori.adventure.text.TextComponent tc ? tc.content() : c.toString();
        }
    }

    private static ActionEntry action(ActionType type) {
        ActionEntry e = new ActionEntry();
        e.setType(type);
        return e;
    }

    private ActionContext context(RecordingPlatform platform, Map<String, String> placeholders) {
        return new ActionContext(null, platform, placeholders, Component::text);
    }

    @Test
    void runsActionsInOrder() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionEntry run = action(ActionType.RUN);
        run.setCommand("spawn");
        ActionEntry msg = action(ActionType.MESSAGE);
        msg.setText("hi");

        ActionSequenceExecutor.run(List.of(run, msg), context(platform, Map.of()));

        assertEquals(List.of("run[PLAYER]:spawn", "message:hi"), platform.log);
    }

    @Test
    void delayBreaksAndResumesTheSequence() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionEntry first = action(ActionType.MESSAGE);
        first.setText("before");
        ActionEntry delay = action(ActionType.DELAY);
        delay.setTicks(20);
        ActionEntry second = action(ActionType.MESSAGE);
        second.setText("after");

        ActionSequenceExecutor.run(List.of(first, delay, second), context(platform, Map.of()));

        assertEquals(List.of("message:before", "delay:20", "message:after"), platform.log);
    }

    @Test
    void placeholdersAreSubstituted() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionEntry run = action(ActionType.RUN);
        run.setCommand("say hello {sender}");
        run.setAs(RunAs.CONSOLE);

        ActionSequenceExecutor.run(List.of(run), context(platform, Map.of("sender", "Steve")));

        assertEquals(List.of("run[CONSOLE]:say hello Steve"), platform.log);
    }

    @Test
    void blankCommandRunIsSkipped() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionEntry run = action(ActionType.RUN); // empty command

        ActionSequenceExecutor.run(List.of(run), context(platform, Map.of()));

        assertTrue(platform.log.isEmpty(), "blank run command should not dispatch");
    }

    @Test
    void trailingDelayIsANoOp() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionEntry msg = action(ActionType.MESSAGE);
        msg.setText("only");
        ActionEntry delay = action(ActionType.DELAY);
        delay.setTicks(100);

        ActionSequenceExecutor.run(List.of(msg, delay), context(platform, Map.of()));

        // A delay with nothing after it should not even schedule.
        assertEquals(List.of("message:only"), platform.log);
    }

    @Test
    void emptySequenceIsNoOp() {
        RecordingPlatform platform = new RecordingPlatform();
        ActionSequenceExecutor.run(List.of(), context(platform, Map.of()));
        assertTrue(platform.log.isEmpty());
    }
}
