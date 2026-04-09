package irden.space.proxy.protocol.payload.connect_failure;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.PacketParser;

public class ConnectFailureParser implements PacketParser<ConnectFailure> {

    @Override
    public ConnectFailure parse(BinaryReader reader) {
        String reason = StarStringCodec.read(reader);
        return new ConnectFailure(reason);
    }

    @Override
    public byte[] write(ConnectFailure payload) {
        BinaryWriter writer = new BinaryWriter();
        StarStringCodec.write(writer, payload.reason());
        return finish(writer);
    }
}
