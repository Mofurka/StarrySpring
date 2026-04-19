package irden.space.proxy.protocol.codec;

public enum StarByteArrayCodec implements BinaryCodec<byte[]> {
    INSTANCE;

    @Override
    public byte[] read(BinaryReader reader) {
        int length = VlqUnsignedCodec.INSTANCE.read(reader);
        return reader.readBytes(length);
    }

    @Override
    public void write(BinaryWriter writer, byte[] value) {
        VlqUnsignedCodec.INSTANCE.write(writer, value.length);
        writer.writeBytes(value);
    }
}