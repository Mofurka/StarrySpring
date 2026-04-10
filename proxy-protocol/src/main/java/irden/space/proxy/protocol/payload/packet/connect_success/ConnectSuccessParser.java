package irden.space.proxy.protocol.payload.packet.connect_success;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ConnectSuccessParser implements PacketParser<ConnectSuccess> {

    @Override
    public ConnectSuccess parse(BinaryReader reader) {
        return new ConnectSuccess(
                VlqCodec.read(reader),
                StarUuidCodec.INSTANCE.read(reader),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE(),
                reader.readInt32BE()
        );
    }

    @Override
    public byte[] write(ConnectSuccess payload) {
        BinaryWriter writer = new BinaryWriter();
        VlqCodec.write(writer, payload.clientId());
        StarUuidCodec.INSTANCE.write(writer, payload.playerUuid());
        writer.writeInt32BE(payload.planetOrbitalLevels());
        writer.writeInt32BE(payload.satelliteOrbitalLevels());
        writer.writeInt32BE(payload.chunkSize());
        writer.writeInt32BE(payload.xyMin());
        writer.writeInt32BE(payload.xyMax());
        writer.writeInt32BE(payload.zMin());
        writer.writeInt32BE(payload.zMax());
        return finish(writer);
    }
}
