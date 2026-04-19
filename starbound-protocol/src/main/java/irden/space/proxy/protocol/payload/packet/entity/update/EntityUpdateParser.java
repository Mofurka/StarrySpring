package irden.space.proxy.protocol.payload.packet.entity.update;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityUpdateParser implements PacketParser<PlayerNetState> {
    @Override
    public PlayerNetState parse(BinaryReader reader) {
        int connectionId = VlqUnsignedCodec.INSTANCE.read(reader);

        int playerEntityid = connectionId * -65536;
        int entityCount = VlqUnsignedCodec.INSTANCE.read(reader);
        // since we dont know what entities type are for update - we will read only player entities, and skip the rest
        // однако позже можно создать хранилище на основе entityCreate и положить уже туда сущности которые были созданы.
        // Так можно будет определить тип сущности при обновлении. Но пока что так.
        for (int i = 0; i < entityCount; i++) {
            int entityId = VlqCodec.INSTANCE.read(reader);
            var raw = StarByteArrayCodec.INSTANCE.read(reader);
            if (entityId < 0) {
                if (entityId == playerEntityid) {
                    var player = PlayerNetState.builder()
                            .entityId(entityId)
                            .connectionId(connectionId);

                    return PlayerEntityUpdateCodec.INSTANCE.read(new BinaryReader(raw, reader.openProtocolVersion()), player);

                }
            }
        }
        return null;


    }

    @Override
    public byte[] write(BinaryWriter writer, PlayerNetState payload) {
        VlqUnsignedCodec.INSTANCE.write(writer, payload.connectionId());// from server
        VlqUnsignedCodec.INSTANCE.write(writer, 1);
        VlqCodec.INSTANCE.write(writer, payload.entityId());
        PlayerEntityUpdateCodec.INSTANCE.write(writer, payload);
        return finish(writer);
    }

}
