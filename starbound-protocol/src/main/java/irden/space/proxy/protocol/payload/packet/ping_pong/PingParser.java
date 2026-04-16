package irden.space.proxy.protocol.payload.packet.ping_pong;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PingParser implements PacketParser<Ping> {
    @Override
    public Ping parse(BinaryReader reader) {
        reader.readBoolean(); //trash
        if (reader.openProtocolVersion() >= 0) {
            return new Ping(VlqUCodec.INSTANCE.read(reader));
        }
        return new Ping(0);
    }

    @Override
    public byte[] write(BinaryWriter writer, Ping payload) {
        writer.writeBoolean(true); //trash
        if (writer.openProtocolVersion() >= 0) {
            VlqUCodec.INSTANCE.write(writer, payload.time());
        }
        return finish(writer);
    }
}
