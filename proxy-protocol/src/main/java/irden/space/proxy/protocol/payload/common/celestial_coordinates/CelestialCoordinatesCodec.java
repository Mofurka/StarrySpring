package irden.space.proxy.protocol.payload.common.celestial_coordinates;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;


public enum CelestialCoordinatesCodec implements BinaryCodec<CelestialCoordinates> {
    INSTANCE;

    @Override
    public CelestialCoordinates read(BinaryReader reader) {
        int x = reader.readInt32BE();
        int y = reader.readInt32BE();
        int z = reader.readInt32BE();
        int worldPlanetId = reader.readInt32BE();
        int worldSatelliteId = reader.readInt32BE();
        return new CelestialCoordinates(x, y, z, worldPlanetId, worldSatelliteId);
    }

    @Override
    public void write(BinaryWriter writer, CelestialCoordinates value) {
        writer.writeInt32BE(value.x());
        writer.writeInt32BE(value.y());
        writer.writeInt32BE(value.z());
        writer.writeInt32BE(value.worldPlanetId());
        writer.writeInt32BE(value.worldSatelliteId());
    }
}
