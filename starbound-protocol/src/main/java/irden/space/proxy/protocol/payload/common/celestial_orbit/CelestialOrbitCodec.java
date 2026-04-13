package irden.space.proxy.protocol.payload.common.celestial_orbit;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3ICodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class CelestialOrbitCodec {

    public static CelestialOrbit read(BinaryReader reader) {
        int direction = reader.readInt32BE();
        double enterTime = reader.readDouble64BE();
        StarVec2F enterPosition = StarVec2FCodec.read(reader);
        return new CelestialOrbit(direction, enterTime, enterPosition);
    }

    public static void write(BinaryWriter writer, CelestialOrbit value) {
        writer.writeInt32BE(value.direction());
        writer.writeDouble64BE(value.enterTime());
        StarVec2FCodec.write(writer, value.enterPosition());
    }
}
