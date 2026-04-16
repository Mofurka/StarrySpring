package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum DamageNotificationCodec implements BinaryCodec<DamageNotification> {
    INSTANCE;

    @Override
    public DamageNotification read(BinaryReader reader) {
        int sourceEntityId = VlqCodec.INSTANCE.read(reader);
        int targetEntityId = VlqCodec.INSTANCE.read(reader);
        StarVec2F position = StarVec2FCodec.INSTANCE.readFixedPointBased(reader, 0.01f);
        float damageDealt = reader.readFloat32BE();
        float healthLost = reader.readFloat32BE();
        HitType hitType = HitType.fromId(reader.readInt32BE());
        String damageSourceKind = StarStringCodec.INSTANCE.read(reader);
        String targetMaterialKind = StarStringCodec.INSTANCE.read(reader);
        return new DamageNotification(
                sourceEntityId,
                targetEntityId,
                position,
                damageDealt,
                healthLost,
                hitType,
                damageSourceKind,
                targetMaterialKind
        );

    }

    @Override
    public void write(BinaryWriter writer, DamageNotification value) {
        VlqCodec.INSTANCE.write(writer, value.sourceEntityId());
        VlqCodec.INSTANCE.write(writer, value.targetEntityId());
        StarVec2FCodec.INSTANCE.writeFixedPointBased(writer, value.position(), 0.01f);
        writer.writeFloat32BE(value.damageDealt());
        writer.writeFloat32BE(value.healthLost());
        writer.writeInt32BE(value.hitType().id());
        StarStringCodec.INSTANCE.write(writer, value.damageSourceKind());
        StarStringCodec.INSTANCE.write(writer, value.targetMaterialKind());
    }
}
