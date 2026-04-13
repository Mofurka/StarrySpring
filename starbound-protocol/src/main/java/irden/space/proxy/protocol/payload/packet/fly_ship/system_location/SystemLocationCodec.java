package irden.space.proxy.protocol.payload.packet.fly_ship.system_location;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinates;
import irden.space.proxy.protocol.payload.common.celestial_coordinates.CelestialCoordinatesCodec;
import irden.space.proxy.protocol.payload.common.celestial_orbit.CelestialOrbit;
import irden.space.proxy.protocol.payload.common.celestial_orbit.CelestialOrbitCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class SystemLocationCodec {
    public static SystemLocation read(BinaryReader reader) {
        int type = reader.readUnsignedByte();
        return switch (type) {
            case 0 -> UniverseSystemLocation.INSTANCE; // When flyship is in the universe, it doesn't have a location, so this is used as a placeholder
            case 1 -> {
                CelestialCoordinates read = CelestialCoordinatesCodec.read(reader);
                yield  new CelestialSystemLocation(read);
            }
            case 2 -> {
                CelestialCoordinates read1 = CelestialCoordinatesCodec.read(reader);
                CelestialOrbit read = CelestialOrbitCodec.read(reader);
                yield new CelestialSystemOrbit(read1, read);
            }
            case 3 -> {
                StarUuid read = StarUuidCodec.read(reader);
                yield new SystemUuid(read);
            }
            case 4 -> {
                StarVec2F read = StarVec2FCodec.read(reader);
                yield new SystemVec2F(read);
            }
            default -> throw new IllegalStateException("Unexpected value 🤨 :" + type);
        };
    }

    public static void write(BinaryWriter writer, SystemLocation value) {
        switch (value) {
            case UniverseSystemLocation _ -> writer.writeByte(0);
            case CelestialSystemLocation(CelestialCoordinates coordinates) -> {
                writer.writeByte(1);
                CelestialCoordinatesCodec.write(writer, coordinates);
            }
            case CelestialSystemOrbit(CelestialCoordinates celestialCoordinates, CelestialOrbit celestialOrbit) -> {
                writer.writeByte(2);
                CelestialCoordinatesCodec.write(writer, celestialCoordinates);
                CelestialOrbitCodec.write(writer, celestialOrbit);
            }
            case SystemUuid(StarUuid systemUuid) -> {
                writer.writeByte(3);
                StarUuidCodec.write(writer, systemUuid);
            }
            case SystemVec2F(StarVec2F coordinates) -> {
                writer.writeByte(4);
                StarVec2FCodec.write(writer, coordinates);
            }
            default -> throw new IllegalStateException("Unexpected value: " + value.getClass().getName());
        }
    }
}
