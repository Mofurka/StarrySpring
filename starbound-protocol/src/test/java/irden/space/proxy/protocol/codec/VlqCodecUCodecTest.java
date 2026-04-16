package irden.space.proxy.protocol.codec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VlqCodecUCodecTest {

    @Test
    void readsMultiByteVlq() {
        BinaryReader reader = new BinaryReader(new byte[]{(byte) 0x81, 0x17});

        assertEquals(151, VlqUCodec.INSTANCE.read(reader));
    }

    @Test
    void writesSignedVlqUsingZigZagEncoding() {
        BinaryWriter writer = new BinaryWriter();
        VlqCodec.INSTANCE.write(writer, -151);

        assertArrayEquals(encodeSignedVlqExpected(-151), writer.toByteArray());
    }

    @Test
    void roundTripsSignedVlqValues() {
        int[] values = {-8192, -300, -151, -1, 0, 1, 63, 64, 127, 128, 8192};

        for (int value : values) {
            BinaryWriter writer = new BinaryWriter();
            VlqCodec.INSTANCE.write(writer, value);

            BinaryReader reader = new BinaryReader(writer.toByteArray());
            assertEquals(value, VlqCodec.INSTANCE.read(reader), "value=" + value);
        }
    }

    private byte[] encodeSignedVlqExpected(int value) {
        int encoded = (value << 1) ^ (value >> 31);
        return encodeVlqExpected(encoded);
    }

    private byte[] encodeVlqExpected(int value) {
        if (value == 0) {
            return new byte[]{0};
        }

        List<Integer> groups = new ArrayList<>();
        int current = value;
        while (current > 0) {
            groups.add(current & 0x7F);
            current >>>= 7;
        }

        byte[] result = new byte[groups.size()];
        for (int i = groups.size() - 1, j = 0; i >= 0; i--, j++) {
            int group = groups.get(i);
            if (i != 0) {
                group |= 0x80;
            }
            result[j] = (byte) group;
        }

        return result;
    }
}

