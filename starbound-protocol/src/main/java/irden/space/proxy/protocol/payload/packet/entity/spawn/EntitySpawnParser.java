package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.EntityType;
import irden.space.proxy.protocol.payload.packet.entity.type.EntityTypeCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntitySpawnParser implements PacketParser<Entity> {


    @Override
    public Entity parse(BinaryReader reader) {
        EntityType entityType = EntityTypeCodec.INSTANCE.read(reader);
        VlqUnsignedCodec.INSTANCE.read(reader); // payload size
        return switch (entityType) {
            case PLANT -> null;
            case OBJECT -> null;
            case VEHICLE -> null;
            case ITEM_DROP -> ItemDropEntitySpawnCodec.INSTANCE.read(reader);
            case PLANT_DROP -> null;
            case PROJECTILE -> null;
            case STAGEHAND -> StagehandEntitySpawnCodec.INSTANCE.read(reader);
            case MONSTER -> null;
            case NPC -> null;
            case PLAYER -> null;
        };
    }

    @Override
    public byte[] write(BinaryWriter writer, Entity payload) {
        return new byte[0];
    }
}
