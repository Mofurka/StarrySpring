package irden.space.proxy.protocol.payload.common.star_uuid;

import irden.space.proxy.protocol.util.HexUtils;

import java.util.Arrays;
import java.util.UUID;

public final class StarUuid {

    private final byte[] value; // always 16 bytes

    public StarUuid(byte[] value) {
        if (value.length != 16) {
            throw new IllegalArgumentException("UUID must be 16 bytes");
        }
        this.value = value.clone();
    }

    public byte[] bytes() {
        return value.clone();
    }

    public String toHex() {
        return HexUtils.toHex(value);
    }

    public static StarUuid fromHex(String hex) {
        return new StarUuid(HexUtils.fromHex(hex));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof StarUuid that)) return false;
        return Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return toHex();
    }

    public UUID toJavaUuid() {
//        return UUID.fromString(toString());
        long mostSigBits = 0;
        long leastSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (value[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (value[i] & 0xFF);
        }
        return new UUID(mostSigBits, leastSigBits);
    }

    public static StarUuid fromJavaUuid(UUID uuid) {
        byte[] bytes = new byte[16];
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (mostSigBits & 0xFF);
            mostSigBits >>= 8;
        }
        for (int i = 15; i >= 8; i--) {
            bytes[i] = (byte) (leastSigBits & 0xFF);
            leastSigBits >>= 8;
        }
        return new StarUuid(bytes);
    }

}
