package irden.space.proxy.protocol.payload.packet.entity_message_response;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityMessageResponseParser implements PacketParser<EntityMessageResponse> {

    @Override
    public EntityMessageResponse parse(BinaryReader reader) {
        return new EntityMessageResponse(
                EntityMessageResponseValueCodec.INSTANCE.read(reader),
                StarUuidCodec.INSTANCE.read(reader)
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, EntityMessageResponse payload) {
        EntityMessageResponseValueCodec.INSTANCE.write(writer, payload.response());
        StarUuidCodec.INSTANCE.write(writer, payload.uuid());
        return finish(writer);
    }
}
