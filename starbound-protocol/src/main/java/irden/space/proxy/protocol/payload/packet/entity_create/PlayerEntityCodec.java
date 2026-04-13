package irden.space.proxy.protocol.payload.packet.entity_create;

import irden.space.proxy.protocol.codec.*;

public enum PlayerEntityCodec implements BinaryCodec<PlayerEntity> {
    INSTANCE;

    @Override
    public PlayerEntity read(BinaryReader reader) {
        byte[] storeDataBytes = StarByteArrayCodec.INSTANCE.read(reader);
        BinaryReader storeDataReader = new BinaryReader(storeDataBytes);

        String uuid = StarStringCodec.INSTANCE.read(storeDataReader);
        String description = StarStringCodec.INSTANCE.read(storeDataReader);
        int modeType = storeDataReader.readInt32BE();
        HumanoidIdentity humanoidIdentity = HumanoidIdentityCodec.INSTANCE.read(storeDataReader);

        byte[] firstNetState = StarByteArrayCodec.INSTANCE.read(reader); // TODO: We need to parse it soon, but its pretty hard due to dynamical inventory. We need to know the scheme first
        int entityId = SignedVlqCodec.INSTANCE.read(reader);
        return new PlayerEntity(uuid, description, modeType, humanoidIdentity, firstNetState, entityId);
    }

    @Override
    public void write(BinaryWriter writer, PlayerEntity value) {
        // TODO: implement
    }
}
