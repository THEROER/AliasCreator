package dev.ua.theroer.commandflow.common.cooldown;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe per-player, per-command cooldown tracker.
 *
 * <p>Records the last-use instant for each {@code (command, player)} pair and
 * answers how much of a cooldown remains. Time is read from an injectable clock
 * (defaults to {@link System#currentTimeMillis}) so it can be driven in tests.</p>
 */
public final class CooldownStore {
    /** Supplies the current time in milliseconds. */
    @FunctionalInterface
    public interface Clock {
        long millis();
    }

    private final Clock clock;
    private final Map<String, Map<UUID, Long>> lastUse = new ConcurrentHashMap<>();

    /**
     * Creates a store using the system clock.
     */
    public CooldownStore() {
        this(System::currentTimeMillis);
    }

    /**
     * Creates a store with a custom clock.
     *
     * @param clock time source
     */
    public CooldownStore(Clock clock) {
        this.clock = clock != null ? clock : System::currentTimeMillis;
    }

    /**
     * Returns the remaining cooldown in seconds (rounded up), or 0 when ready.
     *
     * @param command command/alias key
     * @param player player id
     * @param cooldownSeconds configured cooldown length (<= 0 means no cooldown)
     * @return whole seconds remaining, or 0 when the command may run
     */
    public long remainingSeconds(String command, UUID player, long cooldownSeconds) {
        if (cooldownSeconds <= 0 || command == null || player == null) {
            return 0;
        }
        Map<UUID, Long> perPlayer = lastUse.get(command);
        if (perPlayer == null) {
            return 0;
        }
        Long last = perPlayer.get(player);
        if (last == null) {
            return 0;
        }
        long elapsedMillis = clock.millis() - last;
        long remainingMillis = cooldownSeconds * 1000L - elapsedMillis;
        if (remainingMillis <= 0) {
            return 0;
        }
        return (remainingMillis + 999L) / 1000L;
    }

    /**
     * Records the command as used now by the player, starting its cooldown.
     *
     * @param command command/alias key
     * @param player player id
     */
    public void markUsed(String command, UUID player) {
        if (command == null || player == null) {
            return;
        }
        lastUse.computeIfAbsent(command, key -> new ConcurrentHashMap<>()).put(player, clock.millis());
    }

    /**
     * Clears a player's cooldown for a command (e.g. on manual reset).
     *
     * @param command command/alias key
     * @param player player id
     */
    public void clear(String command, UUID player) {
        Map<UUID, Long> perPlayer = lastUse.get(command);
        if (perPlayer != null && player != null) {
            perPlayer.remove(player);
        }
    }

    /**
     * Clears all tracked cooldowns (e.g. on config reload).
     */
    public void clearAll() {
        lastUse.clear();
    }
}
