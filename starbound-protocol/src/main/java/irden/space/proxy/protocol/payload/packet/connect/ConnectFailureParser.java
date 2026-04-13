package irden.space.proxy.protocol.payload.packet.connect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ConnectFailureParser implements PacketParser<ConnectFailure> {

    @Override
    public ConnectFailure parse(BinaryReader reader, int openProtocolVersion) {
        String reason = StarStringCodec.INSTANCE.read(reader);
        return new ConnectFailure(reason);
    }

    @Override
    public byte[] write(ConnectFailure payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarStringCodec.INSTANCE.write(writer, payload.reason());
        return finish(writer);
    }
}
