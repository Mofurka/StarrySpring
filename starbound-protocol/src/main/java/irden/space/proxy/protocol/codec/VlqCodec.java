package irden.space.proxy.protocol.codec;

public enum VlqCodec implements BinaryCodec<Integer> {
    INSTANCE;

    @Override
    public Integer read(BinaryReader reader) {
        int value = VlqUCodec.INSTANCE.read(reader);
        // ZigZag decoding
        if ((value & 1) == 0) {
            return value >>> 1; // Positive number
        } else {
            return -(value >>> 1) - 1; // Negative number
        }
    }

    @Override
    public void write(BinaryWriter writer, Integer value) {
        // ZigZag encoding
        int encoded = (value << 1) ^ (value >> 31);
        VlqUCodec.INSTANCE.write(writer, encoded);
    }
}