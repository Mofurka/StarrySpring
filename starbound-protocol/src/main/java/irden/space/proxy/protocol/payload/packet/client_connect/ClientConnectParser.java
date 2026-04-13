package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ClientConnectParser implements PacketParser<ClientConnect> {

    @Override
    public ClientConnect parse(BinaryReader reader, int openProtocolVersion) {
        byte[] assetsDigest = StarByteArrayCodec.read(reader);
        boolean allowAssetsMismatch = reader.readBoolean();
        StarUuid playerUuid = StarUuidCodec.read(reader);
        String playerName = StarStringCodec.read(reader);
        String shipSpecies = StarStringCodec.read(reader);
        WorldChunks shipChunks = WorldChunksCodec.read(reader);
        ShipUpgrades shipUpgrades = ShipUpgradesCodec.read(reader);
        boolean introComplete = reader.readBoolean();
        String account = StarStringCodec.read(reader);
        VariantValue info = null;
        if (openProtocolVersion >= 3) {
            info = VariantCodec.read(reader);
        }
        return new ClientConnect(
                assetsDigest,
                allowAssetsMismatch,
                playerUuid,
                playerName,
                shipSpecies,
                shipChunks,
                shipUpgrades,
                introComplete,
                account,
                info
        );
    }

    @Override
    public byte[] write(ClientConnect payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        StarByteArrayCodec.write(writer, payload.assetsDigest());
        writer.writeBoolean(payload.allowAssetsMismatch());
        StarUuidCodec.write(writer, payload.playerUuid());
        StarStringCodec.write(writer, payload.playerName());
        StarStringCodec.write(writer, payload.shipSpecies());
        WorldChunksCodec.write(writer, payload.shipChunks());
        ShipUpgradesCodec.write(writer, payload.shipUpgrades());
        writer.writeBoolean(payload.introComplete());
        StarStringCodec.write(writer, payload.account());
        if (openProtocolVersion >= 3) {
            VariantCodec.write(writer, payload.info());
        }
        return finish(writer);
    }
}

