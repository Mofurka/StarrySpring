package irden.space.proxy.protocol.payload.packet.ping_pong;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PingParser implements PacketParser<Ping> {
    @Override
    public Ping parse(BinaryReader reader, int openProtocolVersion) {
        reader.readBoolean(); //trash
        if (openProtocolVersion >= 0) {
            return new Ping(VlqCodec.read(reader));
        }
        return new Ping(0);
    }

    @Override
    public byte[] write(Ping payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeBoolean(true); //trash
        if (openProtocolVersion >= 0) {
            VlqCodec.write(writer, payload.time());
        }
        return finish(writer);
    }
}
