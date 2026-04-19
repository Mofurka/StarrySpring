package irden.space.proxy.protocol.payload.packet.entity.create;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.packet.entity.type.*;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.Objects;

public class EntityCreateParser implements PacketParser<Entity> {
    @Override
    public Entity parse(BinaryReader reader) {
        EntityType entityType = EntityTypeCodec.INSTANCE.read(reader);
        return switch (entityType) {
            case PLANT, OBJECT, ITEM_DROP, VEHICLE, PLANT_DROP, PROJECTILE, STAGEHAND, MONSTER, NPC ->
                    OtherEntityParser.INSTANCE.read(reader);
            case PLAYER -> PlayerEntityCreateCodec.INSTANCE.read(reader);
        };
    }

    @Override
    public byte[] write(BinaryWriter writer, Entity payload) {
        if (Objects.requireNonNull(payload) instanceof PlayerEntity playerEntity) {
            writer.writeByte((byte) EntityType.PLAYER.id());
            PlayerEntityCreateCodec.INSTANCE.write(writer, playerEntity);
        } else {
            throw new IllegalStateException("Unexpected value: " + payload);
        }
        return finish(writer);
    }
}
