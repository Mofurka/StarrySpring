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
    public FlyShip parse(BinaryReader reader, int openProtocolVersion) {
        StarVec3I system = StarVec3ICodec.read(reader);
        SystemLocation systemLocation = SystemLocationCodec.read(reader);
        VariantValue settings = null;
        if (openProtocolVersion >= 3) {
            settings = VariantCodec.read(reader);
        }
        return new FlyShip(system, systemLocation, settings);
    }

    @Override
    public byte[] write(FlyShip payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarVec3ICodec.write(writer, payload.system());
        SystemLocationCodec.write(writer, payload.location());
        if (openProtocolVersion >= 3) {
            VariantCodec.write(writer, payload.settings());
        }
        return finish(writer);
    }
}
