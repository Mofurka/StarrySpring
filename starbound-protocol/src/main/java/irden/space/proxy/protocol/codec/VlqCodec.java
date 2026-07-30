package irden.space.proxy.protocol.codec;

public enum VlqCodec implements BinaryCodec<Integer> {
    INSTANCE;

    /**
     * Примитивный путь без боксинга — использовать в горячих кодеках (EntityUpdate, variant).
     */
    public int readInt(BinaryReader reader) {
        int value = VlqUnsignedCodec.INSTANCE.readInt(reader);
        // ZigZag decoding
        if ((value & 1) == 0) {
            return value >>> 1; // Positive number
        } else {
            return -(value >>> 1) - 1; // Negative number
        }
    }

    @Override
    public Integer read(BinaryReader reader) {
        return readInt(reader);
    }

    @Override
    public void write(BinaryWriter writer, Integer value) {
        // ZigZag encoding
        int encoded = (value << 1) ^ (value >> 31);
        VlqUnsignedCodec.INSTANCE.write(writer, encoded);
    }
}