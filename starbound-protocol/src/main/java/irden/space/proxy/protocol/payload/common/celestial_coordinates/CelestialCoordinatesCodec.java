package irden.space.proxy.protocol.payload.common.celestial_coordinates;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3ICodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CelestialCoordinatesCodec {

    public static CelestialCoordinates read(BinaryReader reader) {
        StarVec3I location = StarVec3ICodec.read(reader);
        int worldPlanetId = reader.readInt32BE();
        int worldSatelliteId = reader.readInt32BE();
        return new CelestialCoordinates(location, worldPlanetId, worldSatelliteId);
    }

    public static void write(BinaryWriter writer, CelestialCoordinates value) {
        StarVec3ICodec.write(writer, value.location());
        writer.writeInt32BE(value.worldPlanetId());
        writer.writeInt32BE(value.worldSatelliteId());
    }
}
