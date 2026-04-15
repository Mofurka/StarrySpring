package irden.space.proxy.protocol.payload.packet.fly_ship;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec3ICodec;
import irden.space.proxy.protocol.payload.packet.fly_ship.system_location.SystemLocation;
import irden.space.proxy.protocol.payload.packet.fly_ship.system_location.SystemLocationCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class FlyShipParser implements PacketParser<FlyShip> {


    @Override
    public FlyShip parse(BinaryReader reader) {
        StarVec3I system = StarVec3ICodec.INSTANCE.read(reader);
        SystemLocation systemLocation = SystemLocationCodec.INSTANCE.read(reader);
        VariantValue settings = null;
        if (reader.openProtocolVersion() >= 3) {
            settings = VariantCodec.INSTANCE.read(reader);
        }
        return new FlyShip(system, systemLocation, settings);
    }

    @Override
    public byte[] write(BinaryWriter writer, FlyShip payload) {
        StarVec3ICodec.INSTANCE.write(writer, payload.system());
        SystemLocationCodec.INSTANCE.write(writer, payload.location());
        if (writer.openProtocolVersion() >= 3) {
            VariantCodec.INSTANCE.write(writer, payload.settings());
        }
        return finish(writer);
    }
}
