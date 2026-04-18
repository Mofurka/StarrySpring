package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.codec.variant.IntVariantValue;
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

public class RuntimePacketInspector {

    private static final Logger log = LoggerFactory.getLogger(RuntimePacketInspector.class);


    private final PacketDispatcher packetDispatcher;

    public RuntimePacketInspector(PacketDispatcher packetDispatcher) {
        this.packetDispatcher = packetDispatcher;
    }

    public PacketInspectionResult inspect(PacketEnvelope envelope, PacketDirection direction, int openProtocolVersion) {
        if (envelope.packetType() == null) {
            return PacketInspectionResult.empty();
        }

        Object parsed = null;

        if (packetDispatcher != null) {
            try {
                long startTime = System.currentTimeMillis();
                parsed = packetDispatcher.parse(envelope, openProtocolVersion);
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 100) {
                    log.warn(
                            "[{}] parsing took {} ms for rawType={} type={}. Is server overloaded or is this a very large packet?",
                            direction,
                            duration,
                            envelope.rawPacketTypeId(),
                            envelope.packetType()
                    );
                }

            } catch (Exception e) {
                log.info(
                        "[{}] parse failed for rawType={} type={}: {}",
                        direction,
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        e
                );
            }
        }

        SessionTransportMode negotiatedTransportMode = extractNegotiatedTransportMode(envelope, direction, parsed);
        Integer negotiatedOpenProtocolVersion = extractOpenProtocolVersion(envelope, direction, parsed);

        return new PacketInspectionResult(parsed, negotiatedTransportMode, negotiatedOpenProtocolVersion);
    }

    private SessionTransportMode extractNegotiatedTransportMode(
            PacketEnvelope envelope,
            PacketDirection direction,
            Object parsed
    ) {
        if (isNegotiatedProtocolResponse(envelope, direction, parsed)) {
            return null;
        }

        ProtocolResponse protocolResponse = (ProtocolResponse) parsed;
        Map<String, VariantValue> values = extractInfoMap(protocolResponse);
        if (values == null) {
            return null;
        }

        VariantValue compression = values.get("compression");
        if (!(compression instanceof StringVariantValue(String value))) {
            return null;
        }

        if ("Zstd".equalsIgnoreCase(value)) {
            return SessionTransportMode.ZSTD;
        }

        log.debug("Unknown negotiated transport mode '{}' for protocol response", value);
        return null;
    }

    private Integer extractOpenProtocolVersion(
            PacketEnvelope envelope,
            PacketDirection direction,
            Object parsed
    ) {
        if (isNegotiatedProtocolResponse(envelope, direction, parsed)) {
            return null;
        }

        Map<String, VariantValue> values = extractInfoMap((ProtocolResponse) parsed);
        if (values == null) {
            return null;
        }

        VariantValue openProtocolVersion = values.get("openProtocolVersion");
        if (openProtocolVersion instanceof IntVariantValue(int value)) {
            return value;
        }

        return null;
    }

    private boolean isNegotiatedProtocolResponse(PacketEnvelope envelope, PacketDirection direction, Object parsed) {
        return direction != PacketDirection.TO_CLIENT
                || envelope.packetType() != PacketType.PROTOCOL_RESPONSE
                || !(parsed instanceof ProtocolResponse);
    }

    private Map<String, VariantValue> extractInfoMap(ProtocolResponse response) {
        if (response.info() instanceof MapVariantValue(Map<String, VariantValue> values)) {
            return values;
        }

        return null;
    }
}
