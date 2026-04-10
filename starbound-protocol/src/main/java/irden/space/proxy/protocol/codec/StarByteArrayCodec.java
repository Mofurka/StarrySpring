package irden.space.proxy.protocol.codec;

public final class StarByteArrayCodec {

    private StarByteArrayCodec() {
    }

    public static byte[] read(BinaryReader reader) {
        int length = VlqCodec.read(reader);
        return reader.readBytes(length);
    }

    public static void write(BinaryWriter writer, byte[] value) {
        VlqCodec.write(writer, value.length);
        writer.writeBytes(value);
    }
}