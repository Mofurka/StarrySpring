package irden.space.proxy.protocol.codec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public final class BinaryReader {
    private final ByteArrayInputStream input;

    public BinaryReader (byte[] data) {
        this.input = new ByteArrayInputStream(data);
    }

    public int remaining() {
        return input.available();
    }

    public boolean hasRemaining() {
        return input.available() > 0;
    }

    public int readUnsignedByte() {
        int value = input.read();
        if (value == -1) {
            throw new IllegalStateException("Unexpected end of data while reading unsigned byte");
        }
        return value;
    }


    public boolean readBoolean() {
        return readUnsignedByte() != 0;
    }

    public byte[] readBytes(int length) {
        byte[] result;
        try {
            result = input.readNBytes(length);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected error while reading bytes", e);
        }
        if (result.length != length) throw new IllegalStateException("Unexpected end of stream");
        return result;
    }

    public short readInt16BE() {
        int high = readUnsignedByte();
        int low = readUnsignedByte();
        return (short) ((high << 8) | low);
    }

    public int readUInt16BE() {
        int high = readUnsignedByte();
        int low = readUnsignedByte();
        return (high << 8) | low;
    }


    public int readInt32BE() {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        int b4 = readUnsignedByte();
        return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
    }

    public long readUInt32BE() {
        return Integer.toUnsignedLong(readInt32BE());
    }

    public long readInt64BE() {
        long b1 = readUnsignedByte();
        long b2 = readUnsignedByte();
        long b3 = readUnsignedByte();
        long b4 = readUnsignedByte();
        long b5 = readUnsignedByte();
        long b6 = readUnsignedByte();
        long b7 = readUnsignedByte();
        long b8 = readUnsignedByte();
        return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
    }

    public float readFloat32BE() {
        int intBits = readInt32BE();
        return Float.intBitsToFloat(intBits);
    }

    public double readDouble64BE() {
        return Double.longBitsToDouble(readInt64BE());
    }

    public String readString(int length) throws IOException {
        byte[] bytes = readBytes(length);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readRemainingBytes() {
        return input.readAllBytes();
    }
}
