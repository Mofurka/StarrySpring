package irden.space.proxy.protocol.codec;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class BinaryWriter {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public void writeByte(int value) {
        output.write(value & 0xFF);
    }

    public void writeBoolean(boolean value) {
        writeByte(value ? 1 : 0);
    }

    public void writeBytes(byte[] bytes) {
        output.write(bytes, 0, bytes.length);
    }

    public void writeInt16BE(short value) {
        writeByte((value >>> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    public void writeUInt16BE(int value) {
        writeByte((value >>> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    public void writeUInt32BE(int value) {
        writeByte((value >>> 24) & 0xFF);
        writeByte((value >>> 16) & 0xFF);
        writeByte((value >>> 8) & 0xFF);
        writeByte(value & 0xFF);
    }

    public void writeInt32BE(long value) {
        writeUInt32BE((int) value);
    }

    public void writeInt64BE(long value) {
        writeByte((int) ((value >>> 56) & 0xFF));
        writeByte((int) ((value >>> 48) & 0xFF));
        writeByte((int) ((value >>> 40) & 0xFF));
        writeByte((int) ((value >>> 32) & 0xFF));
        writeByte((int) ((value >>> 24) & 0xFF));
        writeByte((int) ((value >>> 16) & 0xFF));
        writeByte((int) ((value >>> 8) & 0xFF));
        writeByte((int) (value & 0xFF));
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
        return output.toByteArray();
    }
}