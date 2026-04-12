package irden.space.proxy.protocol.payload.packet.protocol_request;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ProtocolRequestParser implements PacketParser<ProtocolRequest> {

    @Override
    public ProtocolRequest parse(BinaryReader reader, int openProtocolVersion) {
        return new ProtocolRequest(reader.readUInt32BE());
    }

    @Override
    public byte[] write(ProtocolRequest payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeInt32BE(payload.clientBuild());
        return finish(writer);
    }
}
