package irden.space.proxy.protocol.payload.packet.entity_interact;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.interaction.InteractAction;
import irden.space.proxy.protocol.payload.common.interaction.InteractActionCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityInteractResultParser implements PacketParser<EntityInteractResult> {
    @Override
    public EntityInteractResult parse(BinaryReader reader, int openProtocolVersion) {
        InteractAction action = InteractActionCodec.INSTANCE.read(reader);
        StarUuid requestId = StarUuidCodec.INSTANCE.read(reader);
        return new EntityInteractResult(action, requestId);
    }

    @Override
    public byte[] write(EntityInteractResult payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        InteractActionCodec.INSTANCE.write(writer, payload.interactAction());
        StarUuidCodec.INSTANCE.write(writer, payload.requestId());
        return finish(writer);
    }
}
