package irden.space.proxy.protocol.payload.packet.damage;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.damage.DamageNotification;
import irden.space.proxy.protocol.payload.common.damage.DamageNotificationCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class RemoteDamageNotificationParser implements PacketParser<RemoteDamageNotification> {
    @Override
    public RemoteDamageNotification parse(BinaryReader reader) {
        int entityId = reader.readInt32BE();
        DamageNotification damageNotification = DamageNotificationCodec.INSTANCE.read(reader);
        return new RemoteDamageNotification(entityId, damageNotification);
    }

    @Override
    public byte[] write(BinaryWriter writer, RemoteDamageNotification payload) {
        writer.writeInt32BE(payload.entityId());
        DamageNotificationCodec.INSTANCE.write(writer, payload.damageNotification());
        return finish(writer);
    }
}
