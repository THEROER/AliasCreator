package dev.ua.theroer.commandflow.common.cooldown;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CooldownStoreTest {
    /** Mutable clock so tests advance time deterministically. */
    private static final class FakeClock implements CooldownStore.Clock {
        long now = 0;

        @Override
        public long millis() {
            return now;
        }
    }

    @Test
    void noCooldownWhenUnused() {
        CooldownStore store = new CooldownStore(new FakeClock());
        assertEquals(0, store.remainingSeconds("cmd", UUID.randomUUID(), 60));
    }

    @Test
    void remainingCountsDownAndExpires() {
        FakeClock clock = new FakeClock();
        CooldownStore store = new CooldownStore(clock);
        UUID player = UUID.randomUUID();

        store.markUsed("cmd", player);
        assertEquals(60, store.remainingSeconds("cmd", player, 60));

        clock.now = 10_000; // 10s later
        assertEquals(50, store.remainingSeconds("cmd", player, 60));

        clock.now = 60_000; // exactly elapsed
        assertEquals(0, store.remainingSeconds("cmd", player, 60));
    }

    @Test
    void zeroCooldownNeverBlocks() {
        CooldownStore store = new CooldownStore(new FakeClock());
        UUID player = UUID.randomUUID();
        store.markUsed("cmd", player);
        assertEquals(0, store.remainingSeconds("cmd", player, 0));
    }

    @Test
    void cooldownsArePerPlayerAndPerCommand() {
        FakeClock clock = new FakeClock();
        CooldownStore store = new CooldownStore(clock);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        store.markUsed("kit", a);
        assertEquals(30, store.remainingSeconds("kit", a, 30));
        assertEquals(0, store.remainingSeconds("kit", b, 30), "other player not on cooldown");
        assertEquals(0, store.remainingSeconds("heal", a, 30), "other command not on cooldown");
    }

    @Test
    void clearResetsCooldown() {
        CooldownStore store = new CooldownStore(new FakeClock());
        UUID player = UUID.randomUUID();
        store.markUsed("cmd", player);
        store.clear("cmd", player);
        assertEquals(0, store.remainingSeconds("cmd", player, 60));
    }
}
