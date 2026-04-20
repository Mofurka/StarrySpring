package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.packet.entity.type.ObjectEntity;

public enum ObjectEntitySpawnCodec implements BinaryCodec<ObjectEntity> {
    INSTANCE;

    @Override
    public ObjectEntity read(BinaryReader reader) {
        var name = StarStringCodec.INSTANCE.read(reader);
        var parameters = VariantCodec.INSTANCE.read(reader);
        return new ObjectEntity(name, parameters);
    }

    @Override
    public void write(BinaryWriter writer, ObjectEntity value) {

    }
}
