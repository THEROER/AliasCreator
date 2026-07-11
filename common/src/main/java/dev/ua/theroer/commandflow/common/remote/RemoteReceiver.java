package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;
import java.util.function.BiFunction;

import dev.ua.theroer.magicutils.messaging.MessagingService;
import dev.ua.theroer.magicutils.messaging.Target;

/**
 * Backend-side handler that subscribes to CommandFlow remote channels and acts on
 * them: executing forwarded commands and answering placeholder queries.
 *
 * <p>The platform provides two callbacks — one to dispatch a {@link RemoteCommand}
 * locally, one to resolve a placeholder for a player — so this class stays free of
 * platform types. Installed once per backend after messaging is enabled.</p>
 */
public final class RemoteReceiver {
    /**
     * Runs a forwarded command locally.
     */
    @FunctionalInterface
    public interface CommandExecutor {
        void execute(RemoteCommand command);
    }

    private RemoteReceiver() {
    }

    /**
     * Subscribes the backend to remote command and query channels.
     *
     * @param messaging messaging service
     * @param executor runs a forwarded command locally
     * @param placeholderResolver resolves a placeholder for a player id → value
     *                            (player id may be null for a server-context lookup)
     */
    public static void install(MessagingService messaging, CommandExecutor executor,
                               BiFunction<UUID, String, String> placeholderResolver) {
        messaging.subscribe(RemoteChannels.COMMAND, RemoteCommand.class, message -> {
            RemoteCommand command = message.payload();
            if (command != null && command.getCommandLine() != null) {
                executor.execute(command);
            }
        });

        messaging.subscribe(RemoteChannels.QUERY, PlaceholderQuery.class, message -> {
            PlaceholderQuery query = message.payload();
            if (query == null || query.getPlaceholder() == null) {
                return;
            }
            String value = placeholderResolver.apply(query.playerUuid(), query.getPlaceholder());
            PlaceholderResponse response = new PlaceholderResponse(query.correlationUuid(), value);
            // Broadcast the reply; the requester matches it by the unique
            // correlation id, so no precise reply-addressing is needed.
            messaging.publish(Target.broadcast(), RemoteChannels.RESPONSE, response);
        });
    }
}
