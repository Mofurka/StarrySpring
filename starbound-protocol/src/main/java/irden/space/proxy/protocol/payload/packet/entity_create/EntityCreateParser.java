package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.packet.entity_create.player.PlayerEntityCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityCreateParser implements PacketParser<EntityCreate> {
    @Override
    public EntityCreate parse(BinaryReader reader, int openProtocolVersion) {
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
            case PLAYER -> PlayerEntityCodec.INSTANCE.read(reader);
        };
    }

    @Override
    public byte[] write(EntityCreate payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        switch (payload) {
            case PlayerEntity playerEntity -> {
                writer.writeByte((byte) EntityType.PLAYER.id());
                PlayerEntityCodec.INSTANCE.write(writer, playerEntity);
            }
            default -> throw new IllegalStateException("Unexpected value: " + payload);
        }
        return finish(writer);
    }
}
