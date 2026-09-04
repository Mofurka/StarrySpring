package irden.space.proxy.protocol.codec;

public enum VlqUnsignedCodec implements BinaryCodec<Integer> {
    INSTANCE;

    /**
     * 64 бита по 7 - максимум 10 байт; больше означает мусор в потоке.
     */
    private static final int MAX_BYTES = 10;

    /**
     * Примитивный путь без боксинга — использовать в горячих кодеках (EntityUpdate, variant, строки).
     */
    public long readLong(BinaryReader reader) {
        long value = 0;
        for (int i = 0; i < MAX_BYTES; i++) {
            int b = reader.readUnsignedByte();
            value = (value << 7) | (b & 0x7F);
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new IllegalStateException("VLQ value is longer than " + MAX_BYTES + " bytes");
    }

    /**
     * Для полей, которые по протоколу заведомо влезают в int: размеры, координаты, id сущностей.
     * Значение шире int означает, что мы читаем не с того смещения, и это лучше увидеть сразу,
     * чем молча обрезать и записать наружу другое число.
     */
    public int readInt(BinaryReader reader) {
        long value = readLong(reader);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalStateException("VLQ value does not fit into an int: " + value);
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
        if (value < 0) {
            throw new IllegalArgumentException("VLQ does not support negative values");
        }
        writeBits(writer, value);
    }

    /**
     * Пишет число как 64 беззнаковых бита. Нужно зигзагу из {@link VlqCodec}, у которого
     * старший бит значащий, а не знаковый.
     */
    void writeBits(BinaryWriter writer, long bits) {
        if (bits == 0) {
            writer.writeByte(0);
            return;
        }
        int[] tmp = new int[MAX_BYTES];
        int count = 0;
        long current = bits;
        while (current != 0) {
            tmp[count++] = (int) (current & 0x7F);
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
