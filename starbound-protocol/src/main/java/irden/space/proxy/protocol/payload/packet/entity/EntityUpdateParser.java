package irden.space.proxy.protocol.payload.packet.entity;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class EntityUpdateParser implements PacketParser<PlayerUpdateNetState> {
    @Override
    public PlayerUpdateNetState parse(BinaryReader reader) {
        int connectionId = VlqUCodec.INSTANCE.read(reader);
        int playerEntityid = connectionId * - 65536;
        int entityCount = VlqUCodec.INSTANCE.read(reader);
        // since we dont know what entities type are for update - we will read only player entities, and skip the rest
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
