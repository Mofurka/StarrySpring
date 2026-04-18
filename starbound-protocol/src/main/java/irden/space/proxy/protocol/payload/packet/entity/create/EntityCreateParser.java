package irden.space.proxy.protocol.payload.packet.entity.create;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.packet.entity.player.PlayerEntityCreateCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityCreateParser implements PacketParser<Entity> {
    @Override
    public Entity parse(BinaryReader reader) {
        EntityType entityType = EntityType.fromId(reader.readUnsignedByte());
        return switch (entityType) {
            case PLANT -> null; // TODO: Imlepement all of this
            case OBJECT -> null;
            case VEHICLE -> null;
            case ITEM_DROP -> null;
            case PLANT_DROP -> null;
            case PROJECTILE -> null;
            case STAGEHAND -> null;
            case MONSTER -> null;
            case NPC -> null;
            case PLAYER -> PlayerEntityCreateCodec.INSTANCE.read(reader);
        };
    }

    @Override
    public byte[] write(BinaryWriter writer, Entity payload) {
        switch (payload) {
            case PlayerEntity playerEntity -> {
                writer.writeByte((byte) EntityType.PLAYER.id());
                PlayerEntityCreateCodec.INSTANCE.write(writer, playerEntity);
            }
            default -> throw new IllegalStateException("Unexpected value: " + payload);
        }
        return finish(writer);
    }
}
