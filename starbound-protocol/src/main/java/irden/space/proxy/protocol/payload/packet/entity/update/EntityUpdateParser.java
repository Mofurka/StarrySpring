package irden.space.proxy.protocol.payload.packet.entity.update;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityUpdateParser implements PacketParser<PlayerUpdateNetState> {
    @Override
    public PlayerUpdateNetState parse(BinaryReader reader) {
        int connectionId = VlqUCodec.INSTANCE.read(reader);
        int playerEntityid = connectionId * - 65536;
        int entityCount = VlqUCodec.INSTANCE.read(reader);
        // since we dont know what entities type are for update - we will read only player entities, and skip the rest
        // однако позже можно создать хранилище на основе entityCreate и положить уже туда сущности которые были созданы.
        // Так можно будет определить тип сущности при обновлении. Но пока что так.
        for (int i = 0; i < entityCount; i++) {
            int entityId = VlqCodec.INSTANCE.read(reader);
            var raw = StarByteArrayCodec.INSTANCE.read(reader);
            if (entityId == playerEntityid) {
                return PlayerEntityUpdateCodec.INSTANCE.read(new BinaryReader(raw, reader.openProtocolVersion()));
            }
        }
        return null;


    }

    @Override
    public byte[] write(BinaryWriter writer, PlayerUpdateNetState payload) {
        return new byte[0];
    }

}
