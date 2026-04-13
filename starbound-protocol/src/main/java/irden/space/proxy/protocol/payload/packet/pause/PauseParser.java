package irden.space.proxy.protocol.payload.packet.pause;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PauseParser implements PacketParser<Pause> {
    @Override
    public Pause parse(BinaryReader reader, int openProtocolVersion) {
        boolean pause = reader.readBoolean();
        float timescale = 0.0f;
            if (openProtocolVersion >= 0) {
                timescale = reader.readFloat32BE();
            }
        return new Pause(
                pause, timescale
        );
    }

    @Override
    public byte[] write(Pause payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeBoolean(payload.pause());
        if (openProtocolVersion >= 0) {
            writer.writeFloat32BE(payload.timescale());
        }
        return finish(writer);
    }
}
