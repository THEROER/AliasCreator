package dev.ua.theroer.commandflow.common.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import dev.ua.theroer.magicutils.messaging.LoopbackTransport;
import dev.ua.theroer.magicutils.messaging.MessageSource;
import dev.ua.theroer.magicutils.messaging.MessagingService;
import dev.ua.theroer.magicutils.messaging.Target;
import org.junit.jupiter.api.Test;

/**
 * End-to-end query round-trip over two linked loopback buses: a proxy asks a
 * backend to resolve a placeholder, the backend replies, and the proxy's
 * dispatcher fires the waiting callback matched by correlation id.
 */
class RemoteQueryTest {
    private static MessagingService service(MessageSource self, LoopbackTransport transport) {
        return MessagingService.builder(self).defaultTransport(() -> transport).build();
    }

    @Test
    void queryResolvesThroughBackendReply() {
        LoopbackTransport proxyTransport = new LoopbackTransport();
        LoopbackTransport backendTransport = new LoopbackTransport();
        proxyTransport.link(backendTransport);

        MessagingService proxy = service(MessageSource.proxy("proxy-1"), proxyTransport);
        MessagingService backend = service(MessageSource.backend("backend-1", "survival"), backendTransport);

        // Backend resolves any placeholder to a fixed value for the test.
        RemoteReceiver.install(backend, cmd -> { }, (playerId, placeholder) -> "42");

        MessagingRemoteDispatcher dispatcher = new MessagingRemoteDispatcher(proxy, "proxy-1");

        AtomicReference<String> result = new AtomicReference<>();
        boolean[] timedOut = { false };
        PlaceholderQuery query = new PlaceholderQuery(UUID.randomUUID(), "%balance%", null, null);
        dispatcher.query(RemoteTarget.parse("survival"), query, null, 1000,
                result::set, () -> timedOut[0] = true);

        assertEquals("42", result.get(), "proxy should receive the backend's resolved value");
        assertFalse(timedOut[0]);

        proxy.close();
        backend.close();
    }

    @Test
    void cancelDropsPendingSoLateReplyIgnored() {
        LoopbackTransport a = new LoopbackTransport();
        LoopbackTransport b = new LoopbackTransport();
        a.link(b);
        MessagingService proxy = service(MessageSource.proxy("p"), a);
        MessagingService backend = service(MessageSource.backend("bk", "lobby"), b);

        MessagingRemoteDispatcher dispatcher = new MessagingRemoteDispatcher(proxy, "p");

        // No receiver installed on the backend, so no reply ever comes.
        List<String> results = new CopyOnWriteArrayList<>();
        UUID correlation = UUID.randomUUID();
        PlaceholderQuery query = new PlaceholderQuery(correlation, "%x%", null, null);
        dispatcher.query(RemoteTarget.parse("lobby"), query, null, 1000, results::add, () -> { });

        assertTrue(dispatcher.cancel(correlation), "pending query should be cancellable");
        assertFalse(dispatcher.cancel(correlation), "second cancel is a no-op");

        proxy.close();
        backend.close();
    }

    @Test
    void fireAndForgetCommandReachesBackend() {
        LoopbackTransport a = new LoopbackTransport();
        LoopbackTransport b = new LoopbackTransport();
        a.link(b);
        MessagingService proxy = service(MessageSource.proxy("p"), a);
        MessagingService backend = service(MessageSource.backend("bk", "mini"), b);

        List<String> ran = new CopyOnWriteArrayList<>();
        RemoteReceiver.install(backend, cmd -> ran.add(cmd.getCommandLine()), (id, ph) -> "");

        MessagingRemoteDispatcher dispatcher = new MessagingRemoteDispatcher(proxy, "p");
        RemoteCommand command = new RemoteCommand("spawn", "PLAYER", null, "Steve");
        boolean published = dispatcher.publish(RemoteTarget.parse("mini"), command, null);

        assertTrue(published);
        assertEquals(List.of("spawn"), ran);

        proxy.close();
        backend.close();
    }
}
