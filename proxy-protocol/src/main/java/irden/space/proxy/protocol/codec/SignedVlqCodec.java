package irden.space.proxy.protocol.codec;

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
        int encoded = (value << 1) ^ (value >> 31);
        VlqCodec.write(writer, encoded);
    }
}