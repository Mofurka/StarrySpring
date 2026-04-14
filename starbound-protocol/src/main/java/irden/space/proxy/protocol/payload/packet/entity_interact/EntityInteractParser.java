package irden.space.proxy.protocol.payload.packet.entity_interact;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.interaction.InteractRequest;
import irden.space.proxy.protocol.payload.common.interaction.InteractRequestCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityInteractParser implements PacketParser<EntityInteract> {
    @Override
    public EntityInteract parse(BinaryReader reader, int openProtocolVersion) {
        InteractRequest interactRequest = InteractRequestCodec.INSTANCE.read(reader);
        StarUuid requestId = StarUuidCodec.INSTANCE.read(reader);
        return new EntityInteract(interactRequest, requestId);
    }

    @Override
    public byte[] write(EntityInteract payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        InteractRequestCodec.INSTANCE.write(writer, payload.interactRequest());
        StarUuidCodec.INSTANCE.write(writer, payload.requestId());
        return finish(writer);
    }
}
