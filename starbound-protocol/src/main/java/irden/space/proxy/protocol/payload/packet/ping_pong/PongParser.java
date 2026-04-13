package irden.space.proxy.protocol.payload.packet.ping_pong;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PongParser implements PacketParser<Pong> {
    @Override
    public Pong parse(BinaryReader reader, int openProtocolVersion) {
        reader.readBoolean(); //trash
        if (openProtocolVersion >= 0) {
            return new Pong(VlqCodec.INSTANCE.read(reader));
        }
        return new Pong(0);
    }

    @Override
    public byte[] write(Pong payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeBoolean(true); //trash
        if (openProtocolVersion >= 0) {
            VlqCodec.INSTANCE.write(writer, payload.time());
        }
        return finish(writer);
    }
}
