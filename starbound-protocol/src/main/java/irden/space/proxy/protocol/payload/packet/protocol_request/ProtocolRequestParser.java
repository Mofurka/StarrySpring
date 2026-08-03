package irden.space.proxy.protocol.payload.packet.protocol_request;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ProtocolRequestParser implements PacketParser<ProtocolRequest> {

    @Override
    public ProtocolRequest parse(BinaryReader reader) {
        long clientBuild = reader.readUInt32BE();
        return new ProtocolRequest(clientBuild);
    }

    @Override
    public byte[] write(BinaryWriter writer, ProtocolRequest payload) {
        writer.writeInt32BE(payload.clientBuild());
        return finish(writer);
    }
}
