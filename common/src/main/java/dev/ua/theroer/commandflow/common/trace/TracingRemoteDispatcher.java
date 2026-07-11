package dev.ua.theroer.commandflow.common.trace;

import java.util.UUID;
import java.util.function.Consumer;

import dev.ua.theroer.commandflow.common.remote.PlaceholderQuery;
import dev.ua.theroer.commandflow.common.remote.RemoteCommand;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteTarget;
import org.jetbrains.annotations.Nullable;

/**
 * {@link RemoteDispatcher} that records intended cross-server routing into a
 * {@link TraceRecorder} instead of sending anything. Used by the debug dry-run.
 *
 * <p>A traced query cannot actually reach a backend, so it records the intended
 * lookup and immediately invokes the result callback with a placeholder
 * {@code <query result>} marker, letting the traced sequence continue past a
 * QUERY step without a real round-trip.</p>
 */
public final class TracingRemoteDispatcher implements RemoteDispatcher {
    private static final String DRY_RUN_RESULT = "<query result>";

    private final TraceRecorder recorder;

    /**
     * Creates a tracing dispatcher.
     *
     * @param recorder trace recorder
     */
    public TracingRemoteDispatcher(TraceRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public boolean publish(RemoteTarget target, RemoteCommand command, @Nullable UUID playerId) {
        recorder.record("route RUN", "-> " + describe(target, playerId) + " : /" + command.getCommandLine(),
                TraceStatus.INFO);
        return true;
    }

    @Override
    public void query(RemoteTarget target, PlaceholderQuery query, @Nullable UUID playerId,
                      long timeoutMillis, Consumer<String> onResult, Runnable onTimeout) {
        recorder.record("route QUERY", "-> " + describe(target, playerId) + " : " + query.getPlaceholder(),
                TraceStatus.INFO);
        // Continue the dry-run with a marker value; no real backend to answer.
        onResult.accept(DRY_RUN_RESULT);
    }

    @Override
    public boolean cancel(UUID correlation) {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true; // pretend available so QUERY steps are traced, not skipped
    }

    private static String describe(RemoteTarget target, @Nullable UUID playerId) {
        // RemoteTarget hides its kind; derive a label from the bus target it maps to.
        var busTarget = target.toBusTarget(playerId);
        return busTarget != null ? busTarget.toString() : "local";
    }
}
