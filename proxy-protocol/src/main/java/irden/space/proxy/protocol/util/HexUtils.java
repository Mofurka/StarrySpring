package irden.space.proxy.protocol.util;

public final class HexUtils {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private HexUtils() {
    }

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static byte[] fromHex(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Hex string length must be even");
        }

        byte[] result = new byte[hex.length() / 2];

        for (int i = 0; i < result.length; i++) {
            int index = i * 2;
            int high = Character.digit(hex.charAt(index), 16);
            int low = Character.digit(hex.charAt(index + 1), 16);

            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid hex character");
            }

            result[i] = (byte) ((high << 4) | low);
        }

        return result;
    }
}
