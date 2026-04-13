package irden.space.proxy.protocol.payload.common.damage;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.common.damage.consts.DamageType;
import irden.space.proxy.protocol.payload.common.damage.consts.HitType;
import irden.space.proxy.protocol.payload.common.ephemeral_status_effect.EphemeralStatusEffect;
import irden.space.proxy.protocol.payload.common.ephemeral_status_effect.EphemeralStatusEffectCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

import java.util.List;

public enum DamageRequestCodec implements BinaryCodec<DamageRequest> {
    INSTANCE;

    @Override
    public DamageRequest read(BinaryReader reader) {
        HitType hitType = HitType.fromId(reader.readUInt16BE());
        DamageType type = DamageType.fromId(reader.readUnsignedByte());
        StarVec2F knockbackMomentum = StarVec2FCodec.INSTANCE.read(reader);
        int sourceEntityId = reader.readInt32BE();
        String damageSourceKind = StarStringCodec.INSTANCE.read(reader);
        List<EphemeralStatusEffect> statusEffects = EphemeralStatusEffectCodec.INSTANCE.readList(reader);
        return new DamageRequest(hitType, type, knockbackMomentum, sourceEntityId, damageSourceKind, statusEffects);
    }

    @Override
    public void write(BinaryWriter writer, DamageRequest value) {
        writer.writeUInt16BE(value.hitType().id());
        writer.writeByte(value.type().id());
        StarVec2FCodec.INSTANCE.write(writer, value.knockbackMomentum());
        writer.writeInt32BE(value.sourceEntityId());
        StarStringCodec.INSTANCE.write(writer, value.damageSourceKind());
        EphemeralStatusEffectCodec.INSTANCE.writeList(writer, value.statusEffects());
    }
}
