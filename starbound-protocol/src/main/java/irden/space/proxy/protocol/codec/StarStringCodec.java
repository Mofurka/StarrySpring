package irden.space.proxy.protocol.codec;

import java.nio.charset.StandardCharsets;

public final class StarStringCodec {

    private StarStringCodec() {
    }

    public static String read(BinaryReader reader) {
        byte[] data = StarByteArrayCodec.read(reader);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void write(BinaryWriter writer, String value) {
        StarByteArrayCodec.write(writer, value.getBytes(StandardCharsets.UTF_8));
    }
}