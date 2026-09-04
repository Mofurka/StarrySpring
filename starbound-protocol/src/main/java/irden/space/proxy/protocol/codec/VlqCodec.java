package irden.space.proxy.protocol.codec;

public enum VlqCodec implements BinaryCodec<Integer> {
    INSTANCE;

    /**
     * Примитивный путь без боксинга — использовать в горячих кодеках (EntityUpdate, variant).
     */
    public long readLong(BinaryReader reader) {
        long value = VlqUnsignedCodec.INSTANCE.readLong(reader);
        // ZigZag decoding
        if ((value & 1) == 0) {
            return value >>> 1; // Positive number
        } else {
            return -(value >>> 1) - 1; // Negative number
        }
    }

    /**
     * Для полей, которые по протоколу заведомо влезают в int - см. {@link VlqUnsignedCodec#readInt}.
     */
    public int readInt(BinaryReader reader) {
        long value = readLong(reader);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalStateException("Signed VLQ value does not fit into an int: " + value);
        }
        return (int) value;
    }

    @Override
    public Integer read(BinaryReader reader) {
        return readInt(reader);
    }

    @Override
    public void write(BinaryWriter writer, Integer value) {
        write(writer, (long) value);
    }

    public void write(BinaryWriter writer, long value) {
        // ZigZag encoding: старший бит результата значащий, поэтому пишем его как биты.
        long encoded = (value << 1) ^ (value >> 63);
        VlqUnsignedCodec.INSTANCE.writeBits(writer, encoded);
    }
}
