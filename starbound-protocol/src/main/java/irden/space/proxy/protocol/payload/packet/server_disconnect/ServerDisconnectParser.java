package irden.space.proxy.protocol.payload.packet.server_disconnect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ServerDisconnectParser implements PacketParser<ServerDisconnect> {
    @Override
    public ServerDisconnect parse(BinaryReader reader) {
        String reason = StarStringCodec.read(reader);
        return new ServerDisconnect(reason);
    }

    @Override
    public byte[] write(ServerDisconnect payload) {
        BinaryWriter write = new BinaryWriter();
        StarStringCodec.write(write, payload.reason());
        return finish(write);
    }
}
