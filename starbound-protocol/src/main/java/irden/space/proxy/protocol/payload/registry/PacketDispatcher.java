package irden.space.proxy.protocol.payload.registry;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.packet.PacketEnvelope;

public class PacketDispatcher {

    private final PacketParserRegistry registry;

    public PacketDispatcher(PacketParserRegistry registry) {
        this.registry = registry;
    }

    public Object parse(PacketEnvelope envelope) {
        return parse(envelope, PacketParser.UNKNOWN_OPEN_PROTOCOL_VERSION);
    }

    public Object parse(PacketEnvelope envelope, int openProtocolVersion) {
        if (envelope.packetType() == null) {
            return null;
        }

        PacketParser<?> parser = registry.get(envelope.packetType());
        if (parser == null) {
            return null;
        }

        BinaryReader reader = new BinaryReader(envelope.payload());
        return parser.parse(reader, openProtocolVersion);
    }
}
