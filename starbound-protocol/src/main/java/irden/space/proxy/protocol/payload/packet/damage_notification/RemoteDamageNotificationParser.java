package irden.space.proxy.protocol.payload.packet.damage_notification;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.damage_notification.DamageNotificationCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class RemoteDamageNotificationParser implements PacketParser<RemoteDamageNotification> {
    @Override
    public RemoteDamageNotification parse(BinaryReader reader, int openProtocolVersion) {
        return new RemoteDamageNotification(
                VlqCodec.read(reader),
                DamageNotificationCodec.INSTANCE.read(reader
                )
        );
    }

    @Override
    public byte[] write(RemoteDamageNotification payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        VlqCodec.write(writer, payload.entityId());
        DamageNotificationCodec.INSTANCE.write(writer, payload.damageNotification());
        return finish(writer);
    }
}
