package irden.space.proxy.protocol.payload.common.tile_damage;

public record TileDamage(
        TileDamageType type,
        float amount,
        int harvestLevel
) {
}
