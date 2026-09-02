package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.codec.variant.IntVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketEnvelopes;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import irden.space.proxy.protocol.payload.registry.PacketDispatcher;
import irden.space.proxy.protocol.payload.registry.PacketParserRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RuntimePacketInspectorTest {

    @Test
    void extractsNegotiatedTransportModeAndOpenProtocolVersionFromProtocolResponse() {
        RuntimePacketInspector inspector = new RuntimePacketInspector(new PacketDispatcher(new PacketParserRegistry()));

        Map<String, VariantValue> info = Map.of(
                "compression", new StringVariantValue("Zstd"),
                "openProtocolVersion", new IntVariantValue(13)
        );
        PacketEnvelope envelope = PacketEnvelopes.fromPayload(
                PacketType.PROTOCOL_RESPONSE,
                new ProtocolResponse(1, new MapVariantValue(info)),
                PacketDirection.TO_CLIENT
        );

        PacketInspectionResult inspection = inspector.inspect(envelope, PacketDirection.TO_CLIENT, -1);

        ProtocolResponse parsed = assertInstanceOf(ProtocolResponse.class, inspection.parsed());
        assertEquals(1, parsed.serverResponse());
        assertEquals(SessionTransportMode.ZSTD, inspection.negotiatedTransportMode());
        assertEquals(13, inspection.negotiatedOpenProtocolVersion());
    }

    @Test
    void doesNotParsePayloadUntilItIsRequested() {
        CountingPacketDispatcher dispatcher = new CountingPacketDispatcher();
        RuntimePacketInspector inspector = new RuntimePacketInspector(dispatcher);

        PacketEnvelope envelope = PacketEnvelopes.fromPayload(
                PacketType.CHAT_SENT,
                new ChatSent("hello", ChatSentMode.UNIVERSE, null),
                PacketDirection.TO_SERVER
        );

        PacketInspectionResult inspection = inspector.inspect(envelope, PacketDirection.TO_SERVER, -1);

        assertEquals(0, dispatcher.parseCount(), "осмотр пакета не должен разбирать payload");

        ChatSent parsed = assertInstanceOf(ChatSent.class, inspection.parsed());

        assertEquals("hello", parsed.content());
        assertEquals(1, dispatcher.parseCount());
    }

    @Test
    void parsesProtocolResponseEagerlyBecauseProxyNeedsNegotiatedState() {
        CountingPacketDispatcher dispatcher = new CountingPacketDispatcher();
        RuntimePacketInspector inspector = new RuntimePacketInspector(dispatcher);

        PacketEnvelope envelope = PacketEnvelopes.fromPayload(
                PacketType.PROTOCOL_RESPONSE,
                new ProtocolResponse(1, new MapVariantValue(Map.of("compression", new StringVariantValue("Zstd")))),
                PacketDirection.TO_CLIENT
        );

        PacketInspectionResult inspection = inspector.inspect(envelope, PacketDirection.TO_CLIENT, -1);

        assertEquals(1, dispatcher.parseCount());
        assertEquals(SessionTransportMode.ZSTD, inspection.negotiatedTransportMode());
        assertNotNull(inspection.parsed());
        assertEquals(1, dispatcher.parseCount());
    }

    private static final class CountingPacketDispatcher extends PacketDispatcher {

        private final AtomicInteger parseCount = new AtomicInteger();

        private CountingPacketDispatcher() {
            super(new PacketParserRegistry());
        }

        @Override
        public Object parse(PacketEnvelope envelope, int openProtocolVersion) {
            parseCount.incrementAndGet();
            return super.parse(envelope, openProtocolVersion);
        }

        private int parseCount() {
            return parseCount.get();
        }
    }
}
