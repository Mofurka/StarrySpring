package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;

public enum EntityMessageTargetCodec implements BinaryCodec<EntityMessageTarget> {
    INSTANCE;

    @Override
    public EntityMessageTarget read(BinaryReader reader) {
        boolean targetUnique = reader.readBoolean();
        if (targetUnique) {
            return new UniqueEntityIdTarget(StarUuidCodec.INSTANCE.read(reader));
        }
        return new EntityIdTarget(reader.readInt32BE());
    }

    @Override
    public void write(BinaryWriter writer, EntityMessageTarget target) {
        switch (target) {
            case UniqueEntityIdTarget(StarUuid uniqueEntityId) -> {
                writer.writeBoolean(true);
                StarUuidCodec.INSTANCE.write(writer, uniqueEntityId);
            }
            case EntityIdTarget(int entityId) -> {
                writer.writeBoolean(false);
                writer.writeInt32BE(entityId);
            }
            default -> throw new IllegalStateException("Unsupported entity message target: " + target);
        }
    }
}

