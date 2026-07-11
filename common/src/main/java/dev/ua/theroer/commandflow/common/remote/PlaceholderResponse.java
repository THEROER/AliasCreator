package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;

/**
 * Serializable reply to a {@link PlaceholderQuery}, carrying the resolved value
 * and echoing the query's correlation id.
 */
public final class PlaceholderResponse {
    private String correlationId;
    private String value;

    /**
     * No-arg constructor for deserialization.
     */
    public PlaceholderResponse() {
    }

    /**
     * Creates a response.
     *
     * @param correlationId id from the matching query
     * @param value resolved placeholder value
     */
    public PlaceholderResponse(UUID correlationId, String value) {
        this.correlationId = correlationId != null ? correlationId.toString() : null;
        this.value = value;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return parsed correlation UUID, or null when absent/invalid
     */
    public UUID correlationUuid() {
        if (correlationId == null || correlationId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(correlationId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
