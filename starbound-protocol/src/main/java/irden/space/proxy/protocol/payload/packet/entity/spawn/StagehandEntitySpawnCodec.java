package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;

public enum StagehandEntitySpawnCodec implements BinaryCodec<StageHandEntity> {
    INSTANCE;

    @Override
    public StageHandEntity read(BinaryReader reader) {
        VariantValue payload = VariantCodec.INSTANCE.read(reader);
        return new StageHandEntity(payload, null);
    }

    @Override
    public void write(BinaryWriter writer, StageHandEntity value) {
        throw new UnsupportedOperationException("StagehandEntitySpawn packet is not supported to write. Its useless");
    }
}
