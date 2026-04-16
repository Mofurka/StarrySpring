package irden.space.proxy.protocol.codec;

public enum VlqUCodec implements BinaryCodec<Integer> {
    INSTANCE;

    @Override
    public Integer read(BinaryReader reader) {
        int value = 0;
        while (true) {
            int b = reader.readUnsignedByte();
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                return value;
            }
        }
    }

    @Override
    public void write(BinaryWriter writer, Integer value) {
        if (value < 0) {
            throw new IllegalArgumentException("VLQ does not support negative values");
        }
        if (value == 0) {
            writer.writeByte(0);
            return;
        }
        int[] tmp = new int[10]; // Max 10 bytes for 32-bit int
        int count = 0;
        int current = value;
        while (current > 0) {
            tmp[count++] = current & 0x7F;
            current >>>= 7;
        }
        for (int i = count - 1; i >= 0; i--) {
            int b = tmp[i];
            if (i != 0) {
                b |= 0x80; // Set continuation bit
            }
            writer.writeByte(b);
        }
    }
}