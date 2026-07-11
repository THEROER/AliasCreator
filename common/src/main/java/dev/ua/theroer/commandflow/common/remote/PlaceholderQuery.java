package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;

/**
 * Serializable request asking a backend to resolve a placeholder string in the
 * context of a player, and reply with the resolved value.
 *
 * <p>Carries a {@code correlationId} the requester generated; the backend echoes
 * it in its {@link PlaceholderResponse} so the requester can match the reply to
 * the waiting query.</p>
 */
public final class PlaceholderQuery {
    private String correlationId;
    private String placeholder;
    private String playerId;
    private String replyTo;

    /**
     * No-arg constructor for deserialization.
     */
    public PlaceholderQuery() {
    }

    /**
     * Creates a query.
     *
     * @param correlationId id echoed back in the response
     * @param placeholder placeholder string to resolve on the backend
     * @param playerId player context UUID (may be null)
     * @param replyTo requester member id, so the backend replies to it directly
     */
    public PlaceholderQuery(UUID correlationId, String placeholder, UUID playerId, String replyTo) {
        this.correlationId = correlationId != null ? correlationId.toString() : null;
        this.placeholder = placeholder;
        this.playerId = playerId != null ? playerId.toString() : null;
        this.replyTo = replyTo;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * @return parsed player UUID, or null when absent/invalid
     */
    public UUID playerUuid() {
        return parseUuid(playerId);
    }

    /**
     * @return parsed correlation UUID, or null when absent/invalid
     */
    public UUID correlationUuid() {
        return parseUuid(correlationId);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
