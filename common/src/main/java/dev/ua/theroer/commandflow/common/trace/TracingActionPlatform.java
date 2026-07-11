package dev.ua.theroer.commandflow.common.trace;

import dev.ua.theroer.commandflow.common.action.ActionPlatform;
import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.magicutils.commands.MagicSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/**
 * {@link ActionPlatform} that records intended effects into a {@link TraceRecorder}
 * instead of performing them. Used by {@code /commandflow debug} for a
 * side-effect-free dry-run.
 *
 * <p>{@link #runLater} runs the continuation immediately (no real waiting), so
 * DELAY and QUERY-timeout steps still advance the traced sequence.</p>
 */
public final class TracingActionPlatform implements ActionPlatform {
    private final TraceRecorder recorder;

    /**
     * Creates a tracing platform writing to the given recorder.
     *
     * @param recorder trace recorder
     */
    public TracingActionPlatform(TraceRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void dispatch(MagicSender sender, RunAs runAs, String commandLine) {
        recorder.record("action RUN [" + runAs + "]", "/" + commandLine, TraceStatus.OK);
    }

    @Override
    public void runLater(long ticks, Runnable task) {
        if (ticks > 0) {
            recorder.record("action DELAY", ticks + " ticks", TraceStatus.INFO);
        }
        task.run();
    }

    @Override
    public void message(MagicSender sender, Component message) {
        recorder.record("action MESSAGE", plain(message), TraceStatus.OK);
    }

    @Override
    public void broadcast(Component message) {
        recorder.record("action BROADCAST", plain(message), TraceStatus.OK);
    }

    @Override
    public void sound(MagicSender sender, String sound, float volume, float pitch) {
        recorder.record("action SOUND", sound, TraceStatus.OK);
    }

    @Override
    public void title(MagicSender sender, Component title, Component subtitle,
                      int fadeInTicks, int stayTicks, int fadeOutTicks) {
        recorder.record("action TITLE", plain(title) + " / " + plain(subtitle), TraceStatus.OK);
    }

    @Override
    public void actionBar(MagicSender sender, Component message) {
        recorder.record("action ACTIONBAR", plain(message), TraceStatus.OK);
    }

    private static String plain(Component component) {
        // Our trace messages are built by MiniMessage, whose root is a
        // TextComponent; its content is enough for a debug line.
        return component instanceof TextComponent text ? text.content() : String.valueOf(component);
    }
}
