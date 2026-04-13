package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.damage.consts.DamageType;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.ephemeral_status_effect.EphemeralStatusEffect;
import irden.space.proxy.protocol.payload.common.ephemeral_status_effect.EphemeralStatusEffectCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public final class DamageRequestCodec {


    public static DamageRequest read(BinaryReader reader) {
        HitType hitType = HitType.fromId(reader.readUInt16BE());
        DamageType type = DamageType.fromId(reader.readUnsignedByte());
        StarVec2F knockbackMomentum = StarVec2FCodec.read(reader);
        int sourceEntityId = reader.readInt32BE();
        String damageSourceKind = StarStringCodec.read(reader);
        List<EphemeralStatusEffect> statusEffects = EphemeralStatusEffectCodec.readList(reader);
        return new DamageRequest(hitType, type, knockbackMomentum, sourceEntityId, damageSourceKind, statusEffects);
    }

    public static void write(BinaryWriter writer, DamageRequest value) {
        writer.writeUInt16BE(value.hitType().id());
        writer.writeByte(value.type().id());
        StarVec2FCodec.write(writer, value.knockbackMomentum());
        writer.writeInt32BE(value.sourceEntityId());
        StarStringCodec.write(writer, value.damageSourceKind());
        EphemeralStatusEffectCodec.writeList(writer, value.statusEffects());
    }
}
