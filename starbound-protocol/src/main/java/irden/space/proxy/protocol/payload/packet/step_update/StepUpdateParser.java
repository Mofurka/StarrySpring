package irden.space.proxy.protocol.payload.packet.step_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class StepUpdateParser implements PacketParser<StepUpdate> {
    @Override
    public StepUpdate parse(BinaryReader reader) {
         if (reader.openProtocolVersion() == -1) {
             int steps = VlqCodec.INSTANCE.read(reader);
             return new StepUpdate(
                        steps / 60.0
             );
         }
        return new StepUpdate(
                reader.readDouble64BE()
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, StepUpdate payload) {
        if (writer.openProtocolVersion() == -1) {
            VlqCodec.INSTANCE.write(writer, (int) (payload.remoteTime() * 60));
        } else {
            writer.writeDouble64BE(payload.remoteTime());
        }
        return writer.toByteArray();
    }
}
