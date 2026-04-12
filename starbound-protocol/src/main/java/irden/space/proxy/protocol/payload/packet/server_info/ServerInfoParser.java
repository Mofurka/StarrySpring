package irden.space.proxy.protocol.payload.packet.server_info;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ServerInfoParser implements PacketParser<ServerInfo> {
    @Override
    public ServerInfo parse(BinaryReader reader, int openProtocolVersion) {
        return new ServerInfo(
                reader.readInt32BE(),
                reader.readInt32BE()
        );
    }

    @Override
    public byte[] write(ServerInfo payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeInt32BE(payload.players());
        writer.writeInt32BE(payload.maxPlayers());
        return finish(writer);
    }
}
