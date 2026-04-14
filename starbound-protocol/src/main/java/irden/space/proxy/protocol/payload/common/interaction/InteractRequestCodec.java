package irden.space.proxy.protocol.payload.common.interaction;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum InteractRequestCodec implements BinaryCodec<InteractRequest> {
    INSTANCE;
    @Override
    public InteractRequest read(BinaryReader reader) {
        int entityId = reader.readInt32BE();
        StarVec2F position = StarVec2FCodec.INSTANCE.read(reader);
        int targetId = reader.readInt32BE();
        StarVec2F targetPosition = StarVec2FCodec.INSTANCE.read(reader);
        return new InteractRequest(entityId, position, targetId, targetPosition);
    }

    @Override
    public void write(BinaryWriter writer, InteractRequest value) {
        writer.writeInt32BE(value.entityId());
        StarVec2FCodec.INSTANCE.write(writer, value.position());
        writer.writeInt32BE(value.targetId());
        StarVec2FCodec.INSTANCE.write(writer, value.targetPosition());
    }
}
