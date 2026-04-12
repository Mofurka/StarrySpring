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
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import irden.space.proxy.protocol.payload.registry.PacketDispatcher;
import irden.space.proxy.protocol.payload.registry.PacketParserRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}

