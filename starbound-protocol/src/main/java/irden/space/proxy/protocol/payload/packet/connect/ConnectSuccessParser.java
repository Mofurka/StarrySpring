package irden.space.proxy.protocol.payload.packet.connect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ConnectSuccessParser implements PacketParser<ConnectSuccess> {

    @Override
    public ConnectSuccess parse(BinaryReader reader) {
        int clientId =  VlqUCodec.INSTANCE.read(reader);
        StarUuid serverUuid =  StarUuidCodec.INSTANCE.read(reader); // useless piece of shit
        int planetOrbitalLevels =  reader.readInt32BE();
        int satelliteOrbitalLevels =  reader.readInt32BE();
        int chunkSize =  reader.readInt32BE();
        int xyMin =  reader.readInt32BE();
        int xyMax =  reader.readInt32BE();
        int zMin =  reader.readInt32BE();
        int zMax =  reader.readInt32BE();
        return new ConnectSuccess(
                clientId,
                serverUuid,
                planetOrbitalLevels,
                satelliteOrbitalLevels,
                chunkSize,
                xyMin,
                xyMax,
                zMin,
                zMax
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, ConnectSuccess payload) {
        VlqUCodec.INSTANCE.write(writer, payload.clientId());
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
