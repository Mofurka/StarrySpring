package irden.space.proxy.protocol.payload.packet.entity.type;

import irden.space.proxy.protocol.codec.*;

public enum OtherEntityParser implements BinaryCodec<OtherEntity> {
    INSTANCE;

    @Override
    public OtherEntity read(BinaryReader reader) {
        StarByteArrayCodec.INSTANCE.read(reader);// we dont know what is this, so we will just read and ignore it for now
        StarByteArrayCodec.INSTANCE.read(reader);// we dont know what is this, so we will just read and ignore it for now
        int entityId = VlqUnsignedCodec.INSTANCE.read(reader);
        return new OtherEntity(entityId);
    }

    @Override
    public void write(BinaryWriter writer, OtherEntity value) {
        throw new UnsupportedOperationException("OtherEntity packet is not supported to write. Its useless");
    }
}
