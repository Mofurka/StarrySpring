package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class BinaryWriter {
    private final int openProtocolVersion;
    private byte[] buffer;
    private int position;

    public BinaryWriter() {
        this(PacketParser.LEGACY_PROTOCOL_VERSION);
    }

    public BinaryWriter(int openProtocolVersion) {
        this.openProtocolVersion = openProtocolVersion;
        this.buffer = new byte[64];
    }

    public int openProtocolVersion() {
        return openProtocolVersion;
    }

    public void writeByte(int value) {
        ensureCapacity(1);
        buffer[position++] = (byte) (value & 0xFF);
    }

    public void writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
    }

    public void writeBytes(byte[] bytes) {
        ensureCapacity(bytes.length);
        System.arraycopy(bytes, 0, buffer, position, bytes.length);
        position += bytes.length;
    }

    public void writeInt16BE(short value) {
        ensureCapacity(2);
        buffer[position] = (byte) ((value >>> 8) & 0xFF);
        buffer[position + 1] = (byte) (value & 0xFF);
        position += 2;
    }

    public void writeUInt16BE(int value) {
        ensureCapacity(2);
        buffer[position] = (byte) ((value >>> 8) & 0xFF);
        buffer[position + 1] = (byte) (value & 0xFF);
        position += 2;
    }

    public void writeUInt32BE(int value) {
        ensureCapacity(4);
        buffer[position] = (byte) ((value >>> 24) & 0xFF);
        buffer[position + 1] = (byte) ((value >>> 16) & 0xFF);
        buffer[position + 2] = (byte) ((value >>> 8) & 0xFF);
        buffer[position + 3] = (byte) (value & 0xFF);
        position += 4;
    }

    public void writeInt32BE(long value) {
        writeUInt32BE((int) value);
    }

    public void writeInt64BE(long value) {
        ensureCapacity(8);
        buffer[position] = (byte) ((value >>> 56) & 0xFF);
        buffer[position + 1] = (byte) ((value >>> 48) & 0xFF);
        buffer[position + 2] = (byte) ((value >>> 40) & 0xFF);
        buffer[position + 3] = (byte) ((value >>> 32) & 0xFF);
        buffer[position + 4] = (byte) ((value >>> 24) & 0xFF);
        buffer[position + 5] = (byte) ((value >>> 16) & 0xFF);
        buffer[position + 6] = (byte) ((value >>> 8) & 0xFF);
        buffer[position + 7] = (byte) (value & 0xFF);
        position += 8;
    }

    public void writeFloat32BE(float value) {
        writeUInt32BE(Float.floatToIntBits(value));
    }

    public void writeDouble64BE(double value) {
        writeInt64BE(Double.doubleToLongBits(value));
    }

    public void writeString(String value) {
        writeBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(buffer, position);
    }

    private void ensureCapacity(int additional) {
        int required = position + additional;
        if (required > buffer.length) {
            int newLength = buffer.length;
            do {
                newLength <<= 1;
            } while (newLength < required);
            buffer = Arrays.copyOf(buffer, newLength);
        }
    }
}
