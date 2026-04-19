package irden.space.proxy.protocol.payload.packet.entity.destroy;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityDestroyParser implements PacketParser<EntityDestroy> {
    @Override
    public EntityDestroy parse(BinaryReader reader) {
        int entityId = VlqUnsignedCodec.INSTANCE.read(reader);
        byte[] finalNetState = StarByteArrayCodec.INSTANCE.read(reader);
        boolean death = reader.readBoolean();
        return new EntityDestroy(entityId, finalNetState, death);
    }

    @Override
    public byte[] write(BinaryWriter writer, EntityDestroy payload) {
        throw new UnsupportedOperationException("EntityDestroy packet is not supported to write. Its useless");
    }
}
