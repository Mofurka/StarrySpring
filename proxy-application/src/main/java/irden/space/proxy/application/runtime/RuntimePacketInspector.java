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
    private static final long SLOW_PARSE_THRESHOLD_NANOS = 100_000_000L;
    private static final String COMPRESSION_KEY = "compression";
    private static final String OPEN_PROTOCOL_VERSION_KEY = "openProtocolVersion";

    private final PacketDispatcher packetDispatcher;

    public RuntimePacketInspector(PacketDispatcher packetDispatcher) {
        this.packetDispatcher = packetDispatcher;
    }


    public PacketInspectionResult inspect(PacketEnvelope envelope, PacketDirection direction, int openProtocolVersion) {
        if (envelope.packetType() == null || packetDispatcher == null) {
            return PacketInspectionResult.empty();
        }

        if (!isProtocolNegotiationCandidate(envelope, direction)) {
            return PacketInspectionResult.lazy(() -> parse(envelope, direction, openProtocolVersion));
        }

        Object parsed = parse(envelope, direction, openProtocolVersion);

        SessionTransportMode negotiatedTransportMode = null;
        Integer negotiatedOpenProtocolVersion = null;
        if (parsed instanceof ProtocolResponse protocolResponse) {
            Map<String, VariantValue> values = extractInfoMap(protocolResponse);
            if (values != null) {
                negotiatedTransportMode = extractNegotiatedTransportMode(values);
                negotiatedOpenProtocolVersion = extractOpenProtocolVersion(values);
            }
        }

        Object resolvedPayload = parsed;
        return new PacketInspectionResult(
                () -> resolvedPayload,
                negotiatedTransportMode,
                negotiatedOpenProtocolVersion
        );
    }


    private Object parse(PacketEnvelope envelope, PacketDirection direction, int openProtocolVersion) {
        try {
            long startTime = System.nanoTime();
            Object parsed = packetDispatcher.parse(envelope, openProtocolVersion);
            long durationNanos = System.nanoTime() - startTime;
            if (log.isDebugEnabled()) {
                log.debug(
                        "[{}] parsed packet type={} in {} ms. Payload size={} MB",
                        direction,
                        envelope.packetType(),
                        durationNanos / 1_000_000L,
                        String.format("%.2f", envelope.payload().length / (1024.0 * 1024.0))
                );
            }
            if (durationNanos > SLOW_PARSE_THRESHOLD_NANOS) {
                log.warn(
                        "[{}] parsing took {} ms for rawType={} type={}, size={} MB. Is server overloaded or is this a very large packet?",
                        direction,
                        durationNanos / 1_000_000L,
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        String.format("%.2f", envelope.payload().length / (1024.0 * 1024.0))
                );
            }
            return parsed;
        } catch (Exception e) {
            log.info(
                    "[{}] parse failed for rawType={} type={}:",
                    direction,
                    envelope.rawPacketTypeId(),
                    envelope.packetType(),
                    e
            );
            return null;
        }
    }

    private SessionTransportMode extractNegotiatedTransportMode(Map<String, VariantValue> values) {
        VariantValue compression = values.get(COMPRESSION_KEY);
        if (!(compression instanceof StringVariantValue(String value))) {
            return null;
        }

        if ("Zstd".equalsIgnoreCase(value)) {
            return SessionTransportMode.ZSTD;
        }

        log.debug("Unknown negotiated transport mode '{}' for protocol response", value);
        return null;
    }

    private Integer extractOpenProtocolVersion(Map<String, VariantValue> values) {
        VariantValue openProtocolVersion = values.get(OPEN_PROTOCOL_VERSION_KEY);
        if (openProtocolVersion instanceof IntVariantValue(long value)
                && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }

        return null;
    }

    private boolean isProtocolNegotiationCandidate(PacketEnvelope envelope, PacketDirection direction) {
        return direction == PacketDirection.TO_CLIENT
                && envelope.packetType() == PacketType.PROTOCOL_RESPONSE;
    }

    private Map<String, VariantValue> extractInfoMap(ProtocolResponse response) {
        if (response.info() instanceof MapVariantValue(Map<String, VariantValue> values)) {
            return values;
        }

        return null;
    }
}
