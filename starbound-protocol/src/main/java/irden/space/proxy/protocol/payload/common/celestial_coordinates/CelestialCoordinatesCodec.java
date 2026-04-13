package irden.space.proxy.protocol.payload.common.celestial_coordinates;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3ICodec;

public enum CelestialCoordinatesCodec implements BinaryCodec<CelestialCoordinates> {
    INSTANCE;

    @Override
    public CelestialCoordinates read(BinaryReader reader) {
        StarVec3I location = StarVec3ICodec.INSTANCE.read(reader);
        int worldPlanetId = reader.readInt32BE();
        int worldSatelliteId = reader.readInt32BE();
        return new CelestialCoordinates(location, worldPlanetId, worldSatelliteId);
    }

    @Override
    public void write(BinaryWriter writer, CelestialCoordinates value) {
        StarVec3ICodec.INSTANCE.write(writer, value.location());
        writer.writeInt32BE(value.worldPlanetId());
        writer.writeInt32BE(value.worldSatelliteId());
    }
}
