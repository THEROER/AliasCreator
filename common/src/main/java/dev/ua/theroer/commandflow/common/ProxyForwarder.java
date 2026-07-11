package dev.ua.theroer.commandflow.common;

import java.util.List;
import java.util.UUID;

import dev.ua.theroer.commandflow.common.action.RunAs;
import dev.ua.theroer.commandflow.common.remote.RemoteCommand;
import dev.ua.theroer.commandflow.common.remote.RemoteDispatcher;
import dev.ua.theroer.commandflow.common.remote.RemoteTarget;
import dev.ua.theroer.magicutils.commands.MagicSender;

/**
 * Shared proxy-side forwarding logic: turns an alias invocation on the proxy into
 * a {@link RemoteCommand} routed to the player's own backend server.
 *
 * <p>A proxy does not run an alias's actions/conditions — those belong to the
 * backend hosting the player. So the proxy forwards the raw {@code /<alias> <args>}
 * to that backend, whose CommandFlow evaluates and executes it. The per-platform
 * proxy registrars build a command per alias and call {@link #forward}; the
 * command registration itself stays platform-specific (Velocity/Bungee each have
 * their own {@code CommandRegistry}).</p>
 */
public final class ProxyForwarder {
    /**
     * Outcome of a forward attempt, so the platform can message the player.
     */
    public enum Outcome {
        /** Forwarded to the player's backend. */
        SENT,
        /** The sender is not a player (no server to route to). */
        NOT_A_PLAYER,
        /** Messaging could not route the command (no backend / bus down). */
        UNREACHABLE
    }

    private final RemoteDispatcher remote;

    /**
     * Creates a forwarder.
     *
     * @param remote cross-server dispatcher
     */
    public ProxyForwarder(RemoteDispatcher remote) {
        this.remote = remote;
    }

    /**
     * Forwards an alias invocation to the invoking player's backend.
     *
     * @param alias alias name
     * @param nativeSender platform command sender (wrapped via {@link MagicSender})
     * @param rawArgs raw trailing arguments
     * @return the forward outcome
     */
    public Outcome forward(String alias, Object nativeSender, List<String> rawArgs) {
        MagicSender sender = MagicSender.wrap(nativeSender);
        UUID playerId = sender != null ? sender.id() : null;
        if (playerId == null) {
            return Outcome.NOT_A_PLAYER;
        }
        String tail = rawArgs != null ? String.join(" ", rawArgs).trim() : "";
        String commandLine = tail.isEmpty() ? alias : alias + " " + tail;

        RemoteCommand payload = new RemoteCommand(commandLine, RunAs.PLAYER.name(), playerId, sender.name());
        boolean sent = remote.publish(RemoteTarget.parse("@player"), payload, playerId);
        return sent ? Outcome.SENT : Outcome.UNREACHABLE;
    }
}
