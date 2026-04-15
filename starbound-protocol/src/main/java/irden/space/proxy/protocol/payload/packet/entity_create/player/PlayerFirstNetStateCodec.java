package irden.space.proxy.protocol.payload.packet.entity_create.player;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.damage.consts.TeamType;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2F;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2FCodec;

public enum PlayerFirstNetStateCodec implements BinaryCodec<PlayerFirstNetState> {
    INSTANCE;
    @Override
    @SuppressWarnings("unused")
    public PlayerFirstNetState read(BinaryReader reader) {
            BinaryReader storeDataReader = new BinaryReader(StarByteArrayCodec.INSTANCE.read(reader), reader.openProtocolVersion());
            boolean fullUpdate = storeDataReader.readBoolean();
            int state = VlqCodec.INSTANCE.read(storeDataReader);
            boolean shifting = storeDataReader.readBoolean();
            StarVec2F mousePos = StarVec2FCodec.INSTANCE.readFixedPointBased(storeDataReader, 0.003125f);
            HumanoidIdentity humanoidIdentity = HumanoidIdentityCodec.INSTANCE.read(storeDataReader);
            TeamType teamType = TeamType.fromId(storeDataReader.readUnsignedByte());
            int teamNumber = storeDataReader.readInt16BE();
            boolean landed = storeDataReader.readBoolean();
            String chatMessage = StarStringCodec.INSTANCE.read(storeDataReader);
            boolean newChatMessage = storeDataReader.readBoolean();
            String emote = StarStringCodec.INSTANCE.read(storeDataReader);
            PlayerInventory inventory = PlayerInventoryCodec.INSTANCE.read(storeDataReader);
            return null;
    }

    @Override
    public void write(BinaryWriter writer, PlayerFirstNetState value) {

    }
}
