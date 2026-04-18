package irden.space.proxy.protocol.codec;

import org.junit.jupiter.api.Test;

public class VlqUCodecTest {

    @Test
    public void testEncoding() {
        int[] testValues = {0, 1, 127, 128, 255, 256, 300, 16383, 16384, 2097151};

        for (int val : testValues) {
            BinaryWriter writer = new BinaryWriter();
            VlqUCodec.INSTANCE.write(writer, val);
            byte[] result = writer.toByteArray();

            StringBuilder hex = new StringBuilder();
            for (byte b : result) {
                hex.append(String.format("%02X ", b & 0xFF));
            }
            System.out.printf("Value %7d: %s%n", val, hex.toString().trim());
        }
    }
}

