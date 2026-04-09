package irden.space.proxy.protocol.codec;

import lombok.experimental.UtilityClass;

public final class SignedVlqCodec {

    private SignedVlqCodec() {
    }

    public static int read(BinaryReader reader) {
        int value = VlqCodec.read(reader);
        // ZigZag decoding
        if ((value & 1) == 0) {
            return value >>> 1; // Positive number
        } else {
            return -(value >>> 1) - 1; // Negative number
        }
    }

    public static void write(BinaryWriter writer, int value) {
        // ZigZag encoding
        int encoded = Math.abs(value * 2);
        if (value < 0) {
            encoded = -encoded - 1;
        }
        VlqCodec.write(writer, encoded);
    }
}
