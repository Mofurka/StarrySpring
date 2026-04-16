package irden.space.proxy.protocol.codec;

public enum StarByteArrayCodec implements BinaryCodec<byte[]> {
    INSTANCE;

    @Override
    public byte[] read(BinaryReader reader) {
        int length = VlqUCodec.INSTANCE.read(reader);
        return reader.readBytes(length);
    }

    @Override
    public void write(BinaryWriter writer, byte[] value) {
        VlqUCodec.INSTANCE.write(writer, value.length);
        writer.writeBytes(value);
    }
}