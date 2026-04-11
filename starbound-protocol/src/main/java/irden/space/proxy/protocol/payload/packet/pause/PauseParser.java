package irden.space.proxy.protocol.payload.packet.pause;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PauseParser implements PacketParser<Pause> {
    @Override
    public Pause parse(BinaryReader reader) {
        return new Pause(
                reader.readBoolean(),
                reader.readFloat32BE()
        );
    }

    @Override
    public byte[] write(Pause payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeBoolean(payload.pause());
        writer.writeFloat32BE(payload.timescale());
        return finish(writer);
    }
}
