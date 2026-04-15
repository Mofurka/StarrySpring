package irden.space.proxy.protocol.payload.packet.server_disconnect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ServerDisconnectParser implements PacketParser<ServerDisconnect> {
    @Override
    public ServerDisconnect parse(BinaryReader reader) {
        String reason = StarStringCodec.INSTANCE.read(reader);
        return new ServerDisconnect(reason);
    }

    @Override
    public byte[] write(BinaryWriter writer, ServerDisconnect payload) {
        StarStringCodec.INSTANCE.write(writer, payload.reason());
        return finish(writer);
    }
}
