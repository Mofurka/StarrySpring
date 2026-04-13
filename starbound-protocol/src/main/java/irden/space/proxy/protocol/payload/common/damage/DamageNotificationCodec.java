package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class DamageNotificationCodec {

    public static DamageNotification read(BinaryReader reader) {
        return new DamageNotification(
                VlqCodec.read(reader),
                VlqCodec.read(reader),
                StarVec2FCodec.read(reader),
                reader.readFloat32BE(),
                reader.readFloat32BE(),
                HitType.fromId(reader.readInt32BE()),
                StarStringCodec.read(reader),
                StarStringCodec.read(reader));
    }

    public void write(BinaryWriter writer, DamageNotification value) {
        VlqCodec.write(writer, value.sourceEntityId());
        VlqCodec.write(writer, value.targetEntityId());
        StarVec2FCodec.write(writer, value.position());
        writer.writeFloat32BE(value.damageDealt());
        writer.writeFloat32BE(value.healthLost());
        writer.writeInt32BE(value.hitType().id());
        StarStringCodec.write(writer, value.damageSourceKind());
        StarStringCodec.write(writer, value.targetMaterialKind());
    }
}
