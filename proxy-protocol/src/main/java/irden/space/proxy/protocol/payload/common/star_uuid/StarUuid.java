package irden.space.proxy.protocol.payload.common.star_uuid;

import irden.space.proxy.protocol.util.HexUtils;

import java.util.Arrays;

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
}
