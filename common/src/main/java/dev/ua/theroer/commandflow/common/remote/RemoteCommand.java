package dev.ua.theroer.commandflow.common.remote;

import java.util.UUID;

/**
 * Serializable payload asking a backend server to run a command.
 *
 * <p>Published by a proxy or another backend over the MagicUtils message bus and
 * consumed by the backend receiver, which dispatches {@link #getCommandLine()} as
 * either the invoking player or the console per {@link #getExecutor()}. Plain
 * fields + a no-arg constructor keep it trivially (de)serializable by the bus
 * codec.</p>
 */
public final class RemoteCommand {
    private String commandLine;
    private String executor;
    private String playerId;
    private String playerName;

    /**
     * No-arg constructor for deserialization.
     */
    public RemoteCommand() {
    }

    /**
     * Creates a remote command.
     *
     * @param commandLine command line to run on the backend (no leading slash)
     * @param executor executor identity name ({@code PLAYER}/{@code CONSOLE}/{@code OP})
     * @param playerId invoking player's UUID (may be null for console-only)
     * @param playerName invoking player's name (may be null)
     */
    public RemoteCommand(String commandLine, String executor, UUID playerId, String playerName) {
        this.commandLine = commandLine;
        this.executor = executor;
        this.playerId = playerId != null ? playerId.toString() : null;
        this.playerName = playerName;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(String executor) {
        this.executor = executor;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /**
     * Parses the player UUID, or null when absent/invalid.
     *
     * @return player id, or null
     */
    public UUID playerUuid() {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(playerId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
