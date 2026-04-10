package irden.space.proxy.protocol.payload.packet.step_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class StepUpdateParser implements PacketParser<StepUpdate> {
    @Override
    public StepUpdate parse(BinaryReader reader) {
        return new StepUpdate(
                reader.readDouble64BE()
        );
    }

    @Override
    public byte[] write(StepUpdate payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeDouble64BE(payload.remoteTime());
        return finish(writer);
    }
}
