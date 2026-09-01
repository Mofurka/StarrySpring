package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.DefaultPluginSessionContext;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PacketInterceptionContextTest {

    private static PluginSessionContext session() {
        return new DefaultPluginSessionContext("session-1", "127.0.0.1", false, false);
    }

    private static PacketEnvelope envelope() {
        return new PacketEnvelope(
                PacketType.CHAT_SENT.id(),
                PacketType.CHAT_SENT,
                0,
                false,
                new byte[0],
                new byte[0],
                PacketDirection.TO_SERVER
        );
    }

    @Test
    void resolvesLazyPayloadAtMostOnce() {
        AtomicInteger parseCount = new AtomicInteger();
        PacketInterceptionContext context = PacketInterceptionContext.lazy(
                session(),
                envelope(),
                () -> {
                    parseCount.incrementAndGet();
                    return "chat";
                },
                PacketDirection.TO_SERVER
        );

        assertEquals(0, parseCount.get(), "конструирование контекста не должно разбирать payload");

        assertEquals("chat", context.parsedPayload());
        assertEquals("chat", context.parsedPayload());
        assertEquals("chat", context.parsedPayload(String.class));

        assertEquals(1, parseCount.get());
    }

    @Test
    void cachesNullResultOfFailedParse() {
        AtomicInteger parseCount = new AtomicInteger();
        PacketInterceptionContext context = PacketInterceptionContext.lazy(
                session(),
                envelope(),
                () -> {
                    parseCount.incrementAndGet();
                    return null;
                },
                PacketDirection.TO_SERVER
        );

        assertNull(context.parsedPayload());
        assertNull(context.parsedPayload());

        assertEquals(1, parseCount.get(), "неудачный разбор не должен повторяться на каждом обращении");
    }


    @Test
    void keepsEagerlyProvidedPayload() {
        assertEquals(
                "chat",
                new PacketInterceptionContext(session(), envelope(), "chat", PacketDirection.TO_SERVER)
                        .parsedPayload()
        );
        assertNull(
                new PacketInterceptionContext(session(), null, null, PacketDirection.TO_SERVER)
                        .parsedPayload()
        );
    }

    @Test
    void toStringDoesNotResolvePayload() {
        AtomicInteger parseCount = new AtomicInteger();
        PacketInterceptionContext context = PacketInterceptionContext.lazy(
                session(),
                envelope(),
                () -> {
                    parseCount.incrementAndGet();
                    return "chat";
                },
                PacketDirection.TO_SERVER
        );

        assertTrue(context.toString().contains("CHAT_SENT"));
        assertEquals(0, parseCount.get());
    }
}
