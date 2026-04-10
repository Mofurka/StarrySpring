package irden.space.proxy.protocol.payload.common.damage_notification;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.packet.damage_notification.consts.HitType;

public enum DamageNotificationCodec implements BinaryCodec<DamageNotification> {
    INSTANCE;
    @Override
    public DamageNotification read(BinaryReader reader) {
        return new DamageNotification(
                VlqCodec.read(reader),
                VlqCodec.read(reader),
                StarVec2FCodec.INSTANCE.read(reader),
                reader.readFloat32BE(),
                reader.readFloat32BE(),
                HitType.fromId(reader.readInt32BE()),
                StarStringCodec.read(reader),
                StarStringCodec.read(reader));
    }

    @Override
    public void write(BinaryWriter writer, DamageNotification value) {
        VlqCodec.write(writer, value.sourceEntityId());
        VlqCodec.write(writer, value.targetEntityId());
        StarVec2FCodec.INSTANCE.write(writer, value.position());
        writer.writeFloat32BE(value.damageDealt());
        writer.writeFloat32BE(value.healthLost());
        writer.writeInt32BE(value.hitType().id());
        StarStringCodec.write(writer, value.damageSourceKind());
        StarStringCodec.write(writer, value.targetMaterialKind());
    }
}
