package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class EntityMessageTargetCodec {

    public static EntityMessageTarget read(BinaryReader reader) {
        boolean targetUnique = reader.readBoolean();
        if (targetUnique) {
            return new UniqueEntityIdTarget(StarUuidCodec.read(reader));
        }
        return new EntityIdTarget(reader.readInt32BE());
    }

    public static void write(BinaryWriter writer, EntityMessageTarget target) {
        switch (target) {
            case UniqueEntityIdTarget(StarUuid uniqueEntityId) -> {
                writer.writeBoolean(true);
                StarUuidCodec.write(writer, uniqueEntityId);
            }
            case EntityIdTarget(int entityId) -> {
                writer.writeBoolean(false);
                writer.writeInt32BE(entityId);
            }
            default -> throw new IllegalStateException("Unsupported entity message target: " + target);
        }
    }
}

