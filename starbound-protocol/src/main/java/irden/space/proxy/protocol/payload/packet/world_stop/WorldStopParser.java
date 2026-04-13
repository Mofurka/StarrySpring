package irden.space.proxy.protocol.payload.packet.world_stop;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class WorldStopParser implements PacketParser<WorldStop> {
    @Override
    public WorldStop parse(BinaryReader reader, int openProtocolVersion) {
        String reason = StarStringCodec.INSTANCE.read(reader);
        return new WorldStop(reason);
    }

    @Override
    public byte[] write(WorldStop payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarStringCodec.INSTANCE.write(writer, payload.reason());
        return finish(writer);

    }
}
