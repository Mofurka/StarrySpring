package irden.space.proxy.protocol.codec;

public enum SignedVlqCodec implements BinaryCodec<Integer> {
    INSTANCE;

    @Override
    public Integer read(BinaryReader reader) {
        int value = VlqCodec.INSTANCE.read(reader);
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
        VlqCodec.INSTANCE.write(writer, encoded);
    }
}