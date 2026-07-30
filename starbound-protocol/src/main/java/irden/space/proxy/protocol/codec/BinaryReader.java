package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public final class BinaryReader {
    private final byte[] data;
    private int position;
    private final int openProtocolVersion;

    public BinaryReader(byte[] data, int openProtocolVersion) {
        this.data = data;
        this.openProtocolVersion = openProtocolVersion;
    }

    public BinaryReader(byte[] data) {
        this(data, PacketParser.LEGACY_PROTOCOL_VERSION);
    }

    public int openProtocolVersion() {
        return openProtocolVersion;
    }

    public int remaining() {
        return data.length - position;
    }

    public boolean hasRemaining() {
        return position < data.length;
    }

    public int readUnsignedByte() {
        if (position >= data.length) {
            throw new IllegalStateException("Unexpected end of data while reading unsigned byte");
        }
        return data[position++] & 0xFF;
    }

    public boolean readBoolean() {
        return readUnsignedByte() != 0;
    }

    public byte[] readBytes(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Negative length: " + length);
        }
        if (length > remaining()) {
            throw new IllegalStateException("Unexpected end of stream");
        }
        int from = position;
        position += length;
        return Arrays.copyOfRange(data, from, position);
    }

    public short readInt16BE() {
        return (short) readUInt16BE();
    }

    public int readUInt16BE() {
        ensure(2);
        int value = ((data[position] & 0xFF) << 8)
                | (data[position + 1] & 0xFF);
        position += 2;
        return value;
    }

    public int readInt32BE() {
        ensure(4);
        int value = ((data[position] & 0xFF) << 24)
                | ((data[position + 1] & 0xFF) << 16)
                | ((data[position + 2] & 0xFF) << 8)
                | (data[position + 3] & 0xFF);
        position += 4;
        return value;
    }

    public long readUInt32BE() {
        return Integer.toUnsignedLong(readInt32BE());
    }

    public long readInt64BE() {
        ensure(8);
        long value = ((long) (data[position] & 0xFF) << 56)
                | ((long) (data[position + 1] & 0xFF) << 48)
                | ((long) (data[position + 2] & 0xFF) << 40)
                | ((long) (data[position + 3] & 0xFF) << 32)
                | ((long) (data[position + 4] & 0xFF) << 24)
                | ((long) (data[position + 5] & 0xFF) << 16)
                | ((long) (data[position + 6] & 0xFF) << 8)
                | (data[position + 7] & 0xFF);
        position += 8;
        return value;
    }

    public float readFloat32BE() {
        return Float.intBitsToFloat(readInt32BE());
    }

    public double readDouble64BE() {
        return Double.longBitsToDouble(readInt64BE());
    }

    public String readString(int length) throws IOException {
        byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readRemainingBytes() {
        int from = position;
        position = data.length;
        return Arrays.copyOfRange(data, from, position);
    }

    private void ensure(int count) {
        if (count > remaining()) {
            throw new IllegalStateException("Unexpected end of stream");
        }
    }
}
