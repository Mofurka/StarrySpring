package irden.space.proxy.protocol.payload.packet.universe_time_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class UniverseTimeUpdateParser implements PacketParser<UniverseTimeUpdate> {

    @Override
    public UniverseTimeUpdate parse(BinaryReader reader) {
        double universeTime = VlqUCodec.INSTANCE.read(reader);
        return new UniverseTimeUpdate(universeTime);
    }

    @Override
    public byte[] write(BinaryWriter writer, UniverseTimeUpdate payload) {
        writer.writeDouble64BE(payload.universeTime());
        return finish(writer);
    }
}
