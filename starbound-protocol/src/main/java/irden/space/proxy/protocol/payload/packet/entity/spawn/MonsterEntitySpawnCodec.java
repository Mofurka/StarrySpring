package irden.space.proxy.protocol.payload.packet.entity.spawn;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.entity.type.MonsterEntity;

public enum MonsterEntitySpawnCodec implements BinaryCodec<MonsterEntity> {
    INSTANCE;

    @Override
    public MonsterEntity read(BinaryReader reader) {
        VariantValue payload = VariantCodec.INSTANCE.read(reader);
        return new MonsterEntity(payload);
    }

    @Override
    public void write(BinaryWriter writer, MonsterEntity value) {
        VariantCodec.INSTANCE.write(writer, value.payload());
    }
}
