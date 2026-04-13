package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum DamageNotificationCodec implements BinaryCodec<DamageNotification> {
    INSTANCE;

    @Override
    public DamageNotification read(BinaryReader reader) {
        return new DamageNotification(
                VlqCodec.INSTANCE.read(reader),
                VlqCodec.INSTANCE.read(reader),
                StarVec2FCodec.INSTANCE.read(reader),
                reader.readFloat32BE(),
                reader.readFloat32BE(),
                HitType.fromId(reader.readInt32BE()),
                StarStringCodec.INSTANCE.read(reader),
                StarStringCodec.INSTANCE.read(reader));
    }

    @Override
    public void write(BinaryWriter writer, DamageNotification value) {
        VlqCodec.INSTANCE.write(writer, value.sourceEntityId());
        VlqCodec.INSTANCE.write(writer, value.targetEntityId());
        StarVec2FCodec.INSTANCE.write(writer, value.position());
        writer.writeFloat32BE(value.damageDealt());
        writer.writeFloat32BE(value.healthLost());
        writer.writeInt32BE(value.hitType().id());
        StarStringCodec.INSTANCE.write(writer, value.damageSourceKind());
        StarStringCodec.INSTANCE.write(writer, value.targetMaterialKind());
    }
}
