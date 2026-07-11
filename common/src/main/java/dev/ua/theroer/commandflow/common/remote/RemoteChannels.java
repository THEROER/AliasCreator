package dev.ua.theroer.commandflow.common.remote;

/**
 * Message bus channel names shared by CommandFlow proxy publishers and backend
 * receivers.
 */
public final class RemoteChannels {
    /** Fire-and-forget remote command execution. */
    public static final String COMMAND = "commandflow:command";
    /** Placeholder query request (proxy/backend → backend). */
    public static final String QUERY = "commandflow:query";
    /** Placeholder query reply (backend → requester). */
    public static final String RESPONSE = "commandflow:response";

    private RemoteChannels() {
    }
}
