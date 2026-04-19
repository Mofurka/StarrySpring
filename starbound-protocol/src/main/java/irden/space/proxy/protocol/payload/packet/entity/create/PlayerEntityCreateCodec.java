package irden.space.proxy.protocol.payload.packet.entity.create;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity.type.PlayerEntity;
import irden.space.proxy.protocol.payload.packet.entity.type.player.HumanoidIdentity;
import irden.space.proxy.protocol.payload.packet.entity.type.player.HumanoidIdentityCodec;
import irden.space.proxy.protocol.payload.packet.entity.type.player.PlayerNetState;

public enum PlayerEntityCreateCodec implements BinaryCodec<PlayerEntity> {
    INSTANCE;

    @Override
    public PlayerEntity read(BinaryReader reader) {
        byte[] storeDataBytes = StarByteArrayCodec.INSTANCE.read(reader);
        BinaryReader storeDataReader = new BinaryReader(storeDataBytes);

        StarUuid uuid = StarUuid.fromHex(StarStringCodec.INSTANCE.read(storeDataReader));
        String description = StarStringCodec.INSTANCE.read(storeDataReader);
        int modeType = storeDataReader.readInt32BE();
        HumanoidIdentity humanoidIdentity = HumanoidIdentityCodec.INSTANCE.read(storeDataReader);

        PlayerNetState firstNetState = PlayerFirstNetStateCodec.INSTANCE.read(reader); // We need to parse it soon, but its pretty hard due to dynamical inventory. We need to know the scheme first
        int entityId = VlqCodec.INSTANCE.read(reader);
        return new PlayerEntity(uuid, description, modeType, humanoidIdentity, firstNetState, entityId);
    }

    @Override
    public void write(BinaryWriter writer, PlayerEntity value) {
        BinaryWriter storeDataWriter = new BinaryWriter();
        StarStringCodec.INSTANCE.write(storeDataWriter, value.uuid().toHex());
        StarStringCodec.INSTANCE.write(storeDataWriter, value.description());
        storeDataWriter.writeInt32BE(value.modeType());
        HumanoidIdentityCodec.INSTANCE.write(storeDataWriter, value.humanoidIdentity());

        byte[] storeDataBytes = storeDataWriter.toByteArray();
        StarByteArrayCodec.INSTANCE.write(writer, storeDataBytes);
        PlayerFirstNetStateCodec.INSTANCE.write(writer, value.firstNetState());
        VlqCodec.INSTANCE.write(writer, value.entityId());
    }
}
