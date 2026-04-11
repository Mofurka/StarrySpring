package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.string_set.StringSetCodec;

public enum ShipUpgradesCodec implements BinaryCodec<ShipUpgrades> {
    INSTANCE;

    @Override
    public ShipUpgrades read(BinaryReader reader) {
        return new ShipUpgrades(
                Math.toIntExact(reader.readUInt32BE()),
                Math.toIntExact(reader.readUInt32BE()),
                Math.toIntExact(reader.readUInt32BE()),
                reader.readFloat32BE(),
                reader.readFloat32BE(),
                StringSetCodec.INSTANCE.read(reader)
        );
    }

    @Override
    public void write(BinaryWriter writer, ShipUpgrades value) {
        writeUnsignedInt(writer, value.shipLevel(), "shipLevel");
        writeUnsignedInt(writer, value.maxFuel(), "maxFuel");
        writeUnsignedInt(writer, value.crewSize(), "crewSize");
        writer.writeFloat32BE(value.fuelEfficiency());
        writer.writeFloat32BE(value.shipSpeed());
        StringSetCodec.INSTANCE.write(writer, value.capabilities());
    }

    private static void writeUnsignedInt(BinaryWriter writer, int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be in range of unsigned 32-bit integer");
        }
        writer.writeUInt32BE(value);
    }
}
