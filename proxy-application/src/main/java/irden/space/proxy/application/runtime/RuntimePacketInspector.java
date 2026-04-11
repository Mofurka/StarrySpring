package irden.space.proxy.application.runtime;


import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import irden.space.proxy.protocol.payload.registry.PacketDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class RuntimePacketInspector {

    private static final Logger log = LoggerFactory.getLogger(RuntimePacketInspector.class);

    private static final Set<PacketType> PAYLOAD_LOG_TYPES = Set.of(
//            PacketType.CHAT_SENT,
//            PacketType.CHAT_RECEIVED
    );

    private final PacketDispatcher packetDispatcher;

    public RuntimePacketInspector(PacketDispatcher packetDispatcher) {
        this.packetDispatcher = packetDispatcher;
    }

    public PacketInspectionResult inspect(PacketEnvelope envelope, PacketDirection direction) {
        if (envelope.packetType() == null) {
            return PacketInspectionResult.empty();
        }

        Object parsed = null;

        try {
            parsed = packetDispatcher.parse(envelope);
        } catch (Exception e) {
            log.debug(
                    "[{}] parse failed for rawType={} type={}: {}",
                    direction,
                    envelope.rawPacketTypeId(),
                    envelope.packetType(),
                    e.getMessage()
            );
        }

        boolean negotiatedZstd = direction == PacketDirection.TO_CLIENT
                && envelope.packetType() == PacketType.PROTOCOL_RESPONSE
                && parsed instanceof ProtocolResponse protocolResponse
                && isZstdNegotiated(protocolResponse);

        boolean shouldLogPayload = PAYLOAD_LOG_TYPES.contains(envelope.packetType());

        return new PacketInspectionResult(parsed, negotiatedZstd, shouldLogPayload);
    }

    private boolean isZstdNegotiated(ProtocolResponse response) {
        if (!(response.info() instanceof MapVariantValue(Map<String, VariantValue> values))) {
            return false;
        }

        VariantValue compression = values.get("compression");
        return compression instanceof StringVariantValue(String value)
                && "Zstd".equalsIgnoreCase(value);
    }
}
