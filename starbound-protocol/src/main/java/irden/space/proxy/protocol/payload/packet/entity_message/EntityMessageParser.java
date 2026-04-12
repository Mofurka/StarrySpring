package irden.space.proxy.protocol.payload.packet.entity_message;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityMessageParser implements PacketParser<EntityMessage> {
    @Override
    public EntityMessage parse(BinaryReader reader, int openProtocolVersion) {
        return new EntityMessage(
                EntityMessageTargetCodec.INSTANCE.read(reader),
                StarStringCodec.read(reader),
                VariantCodec.readList(reader),
                StarUuidCodec.INSTANCE.read(reader),
                reader.readUInt16BE()
        );
    }

    @Override
    public byte[] write(EntityMessage payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        EntityMessageTargetCodec.INSTANCE.write(writer, payload.entityId());
        StarStringCodec.write(writer, payload.message());
        VariantCodec.writeList(writer, payload.args());
        StarUuidCodec.INSTANCE.write(writer, payload.uuid());
        writer.writeUInt16BE(payload.fromConnection());
        return finish(writer);
    }
}

