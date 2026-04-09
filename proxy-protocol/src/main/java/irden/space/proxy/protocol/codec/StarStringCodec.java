package irden.space.proxy.protocol.codec;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

public final class StarStringCodec {
    private StarStringCodec() {
    }


    public static String read(BinaryReader reader) {
        int length = VlqCodec.read(reader);
        byte[] bytes = StarByteArrayCodec.read(reader);
        if (bytes.length != length) {
            throw new IllegalStateException("Expected string length " + length + " but got " + bytes.length);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void write(BinaryWriter writer, String value) {
        StarByteArrayCodec.write(writer, value.getBytes(StandardCharsets.UTF_8));
    }
}
