package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.string_set.StringSetCodec;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ShipUpgradesCodec {

    public static ShipUpgrades read(BinaryReader reader) {
        return new ShipUpgrades(
                Math.toIntExact(reader.readUInt32BE()),
                Math.toIntExact(reader.readUInt32BE()),
                Math.toIntExact(reader.readUInt32BE()),
                reader.readFloat32BE(),
                reader.readFloat32BE(),
                StringSetCodec.read(reader)
        );
    }

    public static void write(BinaryWriter writer, ShipUpgrades value) {
        writer.writeUInt32BE(value.shipLevel());
        writer.writeUInt32BE(value.maxFuel());
        writer.writeUInt32BE(value.crewSize());
        writer.writeFloat32BE(value.fuelEfficiency());
        writer.writeFloat32BE(value.shipSpeed());
        StringSetCodec.write(writer, value.capabilities());
    }
}
