package irden.space.proxy.plugin.runtime;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PacketInterceptionChainTest {

    private static PacketInterceptionContext context(PacketType packetType) {
        return new PacketInterceptionContext(
                new DefaultPluginSessionContext("session-1", "127.0.0.1", false, false),
                envelope(packetType),
                "payload-" + packetType,
                PacketDirection.TO_SERVER
        );
    }

    private static PacketEnvelope envelope(PacketType packetType) {
        return new PacketEnvelope(
                packetType.id(),
                packetType,
                0,
                false,
                new byte[0],
                new byte[0],
                PacketDirection.TO_SERVER
        );
    }

    private static PacketInterceptionContext lazyContext(PacketType packetType, AtomicInteger parseCount) {
        return PacketInterceptionContext.lazy(
                new DefaultPluginSessionContext("session-1", "127.0.0.1", false, false),
                envelope(packetType),
                () -> {
                    parseCount.incrementAndGet();
                    return "payload-" + packetType;
                },
                PacketDirection.TO_SERVER
        );
    }

    @Test
    void applyInvokesHandlerRegisteredForConcretePacketType() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        PacketDecision expectedDecision = new ReplacePacketDecision(envelope(PacketType.CHAT_RECEIVE));
        registry.register(PacketType.CHAT_SENT, context -> {
            assertSame(PacketType.CHAT_SENT, context.envelope().packetType());
            return expectedDecision;
        });

        PacketDecision matchedDecision = chain.apply(context(PacketType.CHAT_SENT));
        PacketDecision unmatchedDecision = chain.apply(context(PacketType.CHAT_RECEIVE));

        assertSame(expectedDecision, matchedDecision);
        assertSame(ForwardPacketDecision.INSTANCE, unmatchedDecision);
    }


    @Test
    void applyNeverResolvesPayloadWhenNoInterceptorMatches() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        registry.register(PacketType.CHAT_RECEIVE, context -> context.replaceWithRawPayload(new byte[0]));

        AtomicInteger parseCount = new AtomicInteger();
        PacketDecision decision = chain.apply(lazyContext(PacketType.CHAT_SENT, parseCount));

        assertSame(ForwardPacketDecision.INSTANCE, decision);
        assertEquals(0, parseCount.get());
    }

    @Test
    void applyResolvesPayloadOnceForAllMatchingInterceptors() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        registry.register(PacketType.CHAT_SENT, context -> {
            assertEquals("payload-CHAT_SENT", context.parsedPayload());
            return PacketDecision.forward();
        });
        registry.register(PacketType.CHAT_SENT, context -> {
            assertEquals("payload-CHAT_SENT", context.parsedPayload());
            return PacketDecision.forward();
        });

        AtomicInteger parseCount = new AtomicInteger();
        chain.apply(lazyContext(PacketType.CHAT_SENT, parseCount));

        assertEquals(1, parseCount.get());
    }

    @Test
    void applyRunsAfterForwardCallbacksFromEveryInterceptor() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        List<String> invocations = new ArrayList<>();
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> invocations.add("first")));
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward());
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> invocations.add("third")));

        PacketDecision decision = chain.apply(context(PacketType.CHAT_SENT));

        ForwardPacketDecision forwardDecision = assertInstanceOf(ForwardPacketDecision.class, decision);
        assertNotNull(forwardDecision.afterForward());
        forwardDecision.afterForward().run();
        assertEquals(List.of("first", "third"), invocations);
    }

    @Test
    void applyDiscardsPendingCallbacksWhenPacketIsDropped() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        List<String> invocations = new ArrayList<>();
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> invocations.add("forward")));
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.cancel(() -> invocations.add("drop")));

        PacketDecision decision = chain.apply(context(PacketType.CHAT_SENT));

        DropPacketDecision dropDecision = assertInstanceOf(DropPacketDecision.class, decision);
        assertNotNull(dropDecision.afterDrop());
        dropDecision.afterDrop().run();
        assertEquals(List.of("drop"), invocations);
    }

    @Test
    void applyKeepsPendingCallbacksWhenPacketIsReplaced() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        List<String> invocations = new ArrayList<>();
        PacketEnvelope replacement = envelope(PacketType.CHAT_RECEIVE);
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> invocations.add("forward")));
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.replace(replacement));

        PacketDecision decision = chain.apply(context(PacketType.CHAT_SENT));

        ReplacePacketDecision replaceDecision = assertInstanceOf(ReplacePacketDecision.class, decision);
        assertSame(replacement, replaceDecision.envelope());
        assertNotNull(replaceDecision.afterForward());
        replaceDecision.afterForward().run();
        assertEquals(List.of("forward"), invocations);
    }

    @Test
    void applyRunsRemainingCallbacksWhenOneOfThemFails() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        List<String> invocations = new ArrayList<>();
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> {
            throw new IllegalStateException("boom");
        }));
        registry.register(PacketType.CHAT_SENT, ignored -> PacketDecision.forward(() -> invocations.add("second")));

        PacketDecision decision = chain.apply(context(PacketType.CHAT_SENT));

        ForwardPacketDecision forwardDecision = assertInstanceOf(ForwardPacketDecision.class, decision);
        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> forwardDecision.afterForward().run()
        );

        assertEquals("boom", failure.getMessage());
        assertEquals(List.of("second"), invocations);
    }

    @Test
    void applyInvokesHandlerRegisteredForSeveralPacketTypes() {
        DefaultPacketInterceptorRegistry registry = new DefaultPacketInterceptorRegistry();
        PacketInterceptionChain chain = new PacketInterceptionChain(registry);

        registry.register(List.of(PacketType.CHAT_SENT, PacketType.CHAT_RECEIVE), context -> {
            assertSame(PacketType.CHAT_RECEIVE, context.envelope().packetType());
            return new ReplacePacketDecision(envelope(PacketType.PROTOCOL_RESPONSE));
        });

        PacketDecision chatReceivedDecision = chain.apply(context(PacketType.CHAT_RECEIVE));
        PacketDecision protocolRequestDecision = chain.apply(context(PacketType.PROTOCOL_REQUEST));

        ReplacePacketDecision replaceDecision = assertInstanceOf(ReplacePacketDecision.class, chatReceivedDecision);
        assertSame(PacketType.PROTOCOL_RESPONSE, replaceDecision.envelope().packetType());
        assertSame(ForwardPacketDecision.INSTANCE, protocolRequestDecision);
    }
}

