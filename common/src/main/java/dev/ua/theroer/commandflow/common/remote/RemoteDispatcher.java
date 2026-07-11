package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Sends cross-server commands and placeholder queries over the message bus.
 *
 * <p>Abstracts {@link dev.ua.theroer.magicutils.messaging.MessagingService} so
 * the action engine stays free of the messaging API and so a build without
 * messaging enabled can plug in {@link #DISABLED}. Proxies and backends share the
 * same interface; each wires it to its own {@code MessagingService}.</p>
 */
public interface RemoteDispatcher {
    /**
     * A dispatcher for when cross-server messaging is not available: publishing a
     * command is a no-op and a query fails immediately (timeout callback).
     */
    RemoteDispatcher DISABLED = new RemoteDispatcher() {
        @Override
        public boolean publish(RemoteTarget target, RemoteCommand command, @Nullable UUID playerId) {
            return false;
        }

        @Override
        public void query(RemoteTarget target, PlaceholderQuery query, @Nullable UUID playerId,
                          long timeoutMillis, Consumer<String> onResult, Runnable onTimeout) {
            onTimeout.run();
        }

        @Override
        public boolean cancel(UUID correlation) {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }
    };

    /**
     * Publishes a remote command to the routed backend(s), fire-and-forget.
     *
     * @param target routing derived from the action's {@code server} field
     * @param command command payload
     * @param playerId invoking player id (for {@code @player} routing)
     * @return true when the command was routable and published
     */
    boolean publish(RemoteTarget target, RemoteCommand command, @Nullable UUID playerId);

    /**
     * Sends a placeholder query to a backend and invokes a callback with the
     * resolved value, or a timeout callback if no reply arrives in time.
     *
     * @param target routing (should resolve to a single backend)
     * @param query placeholder query payload
     * @param playerId invoking player id (for {@code @player} routing)
     * @param timeoutMillis how long to wait for a reply
     * @param onResult callback receiving the resolved placeholder value
     * @param onTimeout callback invoked when no reply arrives in time
     */
    void query(RemoteTarget target, PlaceholderQuery query, @Nullable UUID playerId,
               long timeoutMillis, Consumer<String> onResult, Runnable onTimeout);

    /**
     * Cancels a pending query so a late reply is ignored. Called by the engine
     * when the query's timeout elapses.
     *
     * @param correlation correlation id of the query to cancel
     * @return true when a query was still pending (i.e. no reply had arrived)
     */
    boolean cancel(UUID correlation);

    /**
     * @return whether cross-server messaging is currently available
     */
    boolean isAvailable();
}
