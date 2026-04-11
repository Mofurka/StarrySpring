package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record ClientConnect(
        byte[] assetsDigest,
        boolean allowAssetsMismatch,
        StarUuid playerUuid,
        String playerName,
        String shipSpecies,
        WorldChunks shipChunks,
        ShipUpgrades shipUpgrades,
        boolean introComplete,
        String account,
        VariantValue info
) {
}
