package irden.space.proxy.protocol.codec;

import lombok.experimental.UtilityClass;

public final class StarByteArrayCodec {

    private StarByteArrayCodec() {
    }

    public static byte[] read(BinaryReader reader) {
        int length = VlqCodec.read(reader);
        return reader.readBytes(length);
    }

    public static void write(BinaryWriter writer, byte[] data) {
        VlqCodec.write(writer, data.length);
        writer.writeBytes(data);
    }

}
