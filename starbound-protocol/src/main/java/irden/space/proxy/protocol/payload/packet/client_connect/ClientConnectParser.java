package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ClientConnectParser implements PacketParser<ClientConnect> {

    @Override
    public ClientConnect parse(BinaryReader reader) {
        byte[] assetsDigest = StarByteArrayCodec.INSTANCE.read(reader);
        boolean allowAssetsMismatch = reader.readBoolean();
        StarUuid playerUuid = StarUuidCodec.INSTANCE.read(reader);
        String playerName = StarStringCodec.INSTANCE.read(reader);
        String shipSpecies = StarStringCodec.INSTANCE.read(reader);
        WorldChunks shipChunks = WorldChunksCodec.INSTANCE.read(reader);
        ShipUpgrades shipUpgrades = ShipUpgradesCodec.INSTANCE.read(reader);
        boolean introComplete = reader.readBoolean();
        String account = StarStringCodec.INSTANCE.read(reader);
        VariantValue info = null;
        if (reader.openProtocolVersion() >= 3) {
            info = VariantCodec.INSTANCE.read(reader);
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
    public byte[] write(BinaryWriter writer, ClientConnect payload) {
        StarByteArrayCodec.INSTANCE.write(writer, payload.assetsDigest());
        writer.writeBoolean(payload.allowAssetsMismatch());
        StarUuidCodec.INSTANCE.write(writer, payload.playerUuid());
        StarStringCodec.INSTANCE.write(writer, payload.playerName());
        StarStringCodec.INSTANCE.write(writer, payload.shipSpecies());
        WorldChunksCodec.INSTANCE.write(writer, payload.shipChunks());
        ShipUpgradesCodec.INSTANCE.write(writer, payload.shipUpgrades());
        writer.writeBoolean(payload.introComplete());
        StarStringCodec.INSTANCE.write(writer, payload.account());
        if (writer.openProtocolVersion() >= 3) {
            VariantCodec.INSTANCE.write(writer, payload.info());
        }
        return finish(writer);
    }
}

