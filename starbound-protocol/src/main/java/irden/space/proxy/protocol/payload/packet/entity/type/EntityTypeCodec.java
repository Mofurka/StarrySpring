package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum EntityTypeCodec implements BinaryCodec<EntityType> {
    INSTANCE;
    private final Map<Integer, EntityType> idToEntityType = new ConcurrentHashMap<>();

    @Override
    public EntityType read(BinaryReader reader) {
        return EntityType.fromId(reader.readUnsignedByte());
    }

    @Override
    public void write(BinaryWriter writer, EntityType value) {
        writer.writeByte((byte) value.id());
    }

    public void add(int entityId, EntityType entityType) {
        idToEntityType.put(entityId, entityType);
    }

    public EntityType getById(int id) {
        return idToEntityType.get(id);
    }
}
