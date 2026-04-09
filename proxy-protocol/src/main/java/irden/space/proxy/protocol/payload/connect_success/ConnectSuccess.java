package irden.space.proxy.protocol.payload.connect_success;

public record ConnectSuccess(
        int clientId,
        String uuid,
        int planetOrbitalLevels,
        int satelliteOrbitalLevels,
        int chunkSize,
        int xyMin,
        int xyMax,
        int zMin,
        int zMax
) {
}
