package irden.space.proxy.protocol.payload.protocol_request;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.PacketParser;

public class ProtocolRequestParser implements PacketParser<ProtocolRequest> {

    @Override
    public ProtocolRequest parse(BinaryReader reader) {
        return new ProtocolRequest(reader.readUInt32BE());
    }

    @Override
    public byte[] write(ProtocolRequest payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeInt32BE(payload.clientBuild());
        return finish(writer);
    }
}
