package dev.ua.theroer.commandflow.common.remote;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import dev.ua.theroer.magicutils.messaging.MessagingService;
import dev.ua.theroer.magicutils.messaging.Target;
import org.jetbrains.annotations.Nullable;

/**
 * {@link RemoteDispatcher} backed by a MagicUtils {@link MessagingService}.
 *
 * <p>Fire-and-forget commands publish on {@link RemoteChannels#COMMAND}. Queries
 * publish on {@link RemoteChannels#QUERY} and register a pending entry keyed by a
 * fresh correlation id; a subscription on {@link RemoteChannels#RESPONSE} matches
 * replies back and fires the waiting callback. Timeouts are the caller's job (the
 * action engine schedules them) — {@link #completeTimedOut} drops a stale entry.</p>
 */
public final class MessagingRemoteDispatcher implements RemoteDispatcher {
    private final MessagingService messaging;
    private final String selfId;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    /**
     * Creates the dispatcher and subscribes to responses.
     *
     * @param messaging messaging service
     * @param selfId this member's id (used as reply-to address)
     */
    public MessagingRemoteDispatcher(MessagingService messaging, String selfId) {
        this.messaging = messaging;
        this.selfId = selfId;
        messaging.subscribe(RemoteChannels.RESPONSE, PlaceholderResponse.class, message -> {
            PlaceholderResponse response = message.payload();
            if (response == null) {
                return;
            }
            UUID correlation = response.correlationUuid();
            if (correlation == null) {
                return;
            }
            Consumer<String> waiter = pending.remove(correlation);
            if (waiter != null) {
                waiter.accept(response.getValue() != null ? response.getValue() : "");
            }
        });
    }

    @Override
    public boolean publish(RemoteTarget target, RemoteCommand command, @Nullable UUID playerId) {
        Target busTarget = target.toBusTarget(playerId);
        if (busTarget == null) {
            return false;
        }
        messaging.publish(busTarget, RemoteChannels.COMMAND, command);
        return true;
    }

    @Override
    public void query(RemoteTarget target, PlaceholderQuery query, @Nullable UUID playerId,
                      long timeoutMillis, Consumer<String> onResult, Runnable onTimeout) {
        Target busTarget = target.toBusTarget(playerId);
        UUID correlation = query.correlationUuid();
        if (busTarget == null || correlation == null) {
            onTimeout.run();
            return;
        }
        query.setReplyTo(selfId);
        pending.put(correlation, onResult);
        messaging.publish(busTarget, RemoteChannels.QUERY, query);
    }

    @Override
    public boolean cancel(UUID correlation) {
        return correlation != null && pending.remove(correlation) != null;
    }

    @Override
    public boolean isAvailable() {
        return messaging.isConnected();
    }
}
