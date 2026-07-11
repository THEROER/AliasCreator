package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;

import dev.ua.theroer.magicutils.messaging.Target;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed form of an action's {@code server} field, describing where its command
 * should run, plus the mapping to a MagicUtils bus {@link Target}.
 *
 * <p>Config values: empty → local (no routing); {@code *} → every backend;
 * {@code @player} → the backend hosting the invoking player; anything else → a
 * named backend server.</p>
 */
public final class RemoteTarget {
    private enum Kind { LOCAL, ALL, PLAYER_SERVER, NAMED }

    private final Kind kind;
    private final @Nullable String serverName;

    private RemoteTarget(Kind kind, @Nullable String serverName) {
        this.kind = kind;
        this.serverName = serverName;
    }

    /**
     * Parses a config {@code server} value.
     *
     * @param value raw config value (may be null/blank)
     * @return parsed target
     */
    public static RemoteTarget parse(String value) {
        if (value == null || value.isBlank()) {
            return new RemoteTarget(Kind.LOCAL, null);
        }
        String trimmed = value.trim();
        if (trimmed.equals("*")) {
            return new RemoteTarget(Kind.ALL, null);
        }
        if (trimmed.equalsIgnoreCase("@player")) {
            return new RemoteTarget(Kind.PLAYER_SERVER, null);
        }
        return new RemoteTarget(Kind.NAMED, trimmed);
    }

    /**
     * @return true when the command should run locally (no cross-server routing)
     */
    public boolean isLocal() {
        return kind == Kind.LOCAL;
    }

    /**
     * Maps this target to a MagicUtils bus target.
     *
     * @param playerId invoking player id (for {@code @player} routing)
     * @return bus target, or null when local / not routable
     */
    public @Nullable Target toBusTarget(@Nullable UUID playerId) {
        return switch (kind) {
            case LOCAL -> null;
            case ALL -> Target.allBackends();
            case NAMED -> serverName != null ? Target.server(serverName) : null;
            case PLAYER_SERVER -> playerId != null ? Target.player(playerId) : null;
        };
    }
}
