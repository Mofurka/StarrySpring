package irden.space.proxy.protocol.payload.packet.connect_success;

import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;

public record ConnectSuccess(
        int clientId,
        StarUuid playerUuid,
        int planetOrbitalLevels,
        int satelliteOrbitalLevels,
        int chunkSize,
        int xyMin,
        int xyMax,
        int zMin,
        int zMax
) {
}
