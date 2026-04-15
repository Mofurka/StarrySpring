package irden.space.proxy.protocol.payload.packet.pause;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class PauseParser implements PacketParser<Pause> {
    @Override
    public Pause parse(BinaryReader reader) {
        boolean pause = reader.readBoolean();
        float timescale = 0.0f;
            if (reader.openProtocolVersion() >= 0) {
                timescale = reader.readFloat32BE();
            }
        return new Pause(
                pause, timescale
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, Pause payload) {
        writer.writeBoolean(payload.pause());
        if (writer.openProtocolVersion() >= 0) {
            writer.writeFloat32BE(payload.timescale());
        }
        return finish(writer);
    }
}
