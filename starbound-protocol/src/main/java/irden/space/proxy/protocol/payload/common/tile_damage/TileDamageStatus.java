package irden.space.proxy.protocol.payload.common.tile_damage;

import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;

public record TileDamageStatus(
        float damagePercentage,
        float damageEffectTimeFactor,
        boolean harvested,
        StarVec2F damageSourcePosition,
        TileDamageType damageType
) {

    public float damageEffectPercentage() {
        return Math.clamp(damageEffectTimeFactor, 0.0f, 1.0f) * damagePercentage;
    }
}
