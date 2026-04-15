package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum PersonalityCodec implements BinaryCodec<Personality> {
    INSTANCE;

    @Override
    public Personality read(BinaryReader reader) {
        String idle = StarStringCodec.INSTANCE.read(reader);
        String armIdle = StarStringCodec.INSTANCE.read(reader);
        StarVec2F headOffset = StarVec2FCodec.INSTANCE.read(reader);
        StarVec2F armOffset = StarVec2FCodec.INSTANCE.read(reader);
        return new Personality(idle, armIdle, headOffset, armOffset);
    }

    @Override
    public void write(BinaryWriter writer, Personality value) {
        StarStringCodec.INSTANCE.write(writer, value.idle());
        StarStringCodec.INSTANCE.write(writer, value.armIdle());
        StarVec2FCodec.INSTANCE.write(writer, value.headOffset());
        StarVec2FCodec.INSTANCE.write(writer, value.armOffset());
    }
}
