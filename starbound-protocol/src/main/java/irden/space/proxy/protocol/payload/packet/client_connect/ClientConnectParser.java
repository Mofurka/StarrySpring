package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarByteArrayCodec;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuidCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ClientConnectParser implements PacketParser<ClientConnect> {

    @Override
    public ClientConnect parse(BinaryReader reader) {
        return new ClientConnect(
                StarByteArrayCodec.read(reader),
                reader.readBoolean(),
                StarUuidCodec.INSTANCE.read(reader),
                StarStringCodec.read(reader),
                StarStringCodec.read(reader),
                WorldChunksCodec.INSTANCE.read(reader),
                ShipUpgradesCodec.INSTANCE.read(reader),
                reader.readBoolean(),
                StarStringCodec.read(reader),
                VariantCodec.read(reader)
        );
    }

    @Override
    public byte[] write(ClientConnect payload) {
        BinaryWriter writer = new BinaryWriter();
        StarByteArrayCodec.write(writer, payload.assetsDigest());
        writer.writeBoolean(payload.allowAssetsMismatch());
        StarUuidCodec.INSTANCE.write(writer, payload.playerUuid());
        StarStringCodec.write(writer, payload.playerName());
        StarStringCodec.write(writer, payload.shipSpecies());
        WorldChunksCodec.INSTANCE.write(writer, payload.shipChunks());
        ShipUpgradesCodec.INSTANCE.write(writer, payload.shipUpgrades());
        writer.writeBoolean(payload.introComplete());
        StarStringCodec.write(writer, payload.account());
        VariantCodec.write(writer, payload.info());
        return finish(writer);
    }
}

