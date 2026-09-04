package irden.space.proxy.protocol.codec;

import irden.space.proxy.protocol.codec.variant.IntVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VlqLongCodecTest {

    private static final long SNOWFLAKE = 1544983239955845161L;

    @Test
    void roundTripsSignedValuesWiderThanInt() {
        long[] values = {
                Long.MIN_VALUE, Long.MIN_VALUE + 1, -SNOWFLAKE, Integer.MIN_VALUE - 1L,
                -1, 0, 1, Integer.MAX_VALUE + 1L, SNOWFLAKE, Long.MAX_VALUE
        };

        for (long value : values) {
            BinaryWriter writer = new BinaryWriter();
            VlqCodec.INSTANCE.write(writer, value);

            BinaryReader reader = new BinaryReader(writer.toByteArray());
            assertEquals(value, VlqCodec.INSTANCE.readLong(reader), "value=" + value);
        }
    }

    @Test
    void roundTripsUnsignedValuesWiderThanInt() {
        long[] values = {0, 1, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, SNOWFLAKE, Long.MAX_VALUE};

        for (long value : values) {
            BinaryWriter writer = new BinaryWriter();
            VlqUnsignedCodec.INSTANCE.write(writer, value);

            BinaryReader reader = new BinaryReader(writer.toByteArray());
            assertEquals(value, VlqUnsignedCodec.INSTANCE.readLong(reader), "value=" + value);
        }
    }

    @Test
    void carriesLongThroughIntVariant() {
        BinaryWriter writer = new BinaryWriter();
        VariantCodec.INSTANCE.write(writer, Variants.of(SNOWFLAKE));

        VariantValue read = VariantCodec.INSTANCE.read(new BinaryReader(writer.toByteArray()));

        assertEquals(new IntVariantValue(SNOWFLAKE), read);
        assertEquals(SNOWFLAKE, Variants.asLong(read).orElseThrow());
    }

    @Test
    void reportsIntFieldsThatDoNotFitInsteadOfTruncating() {
        BinaryWriter writer = new BinaryWriter();
        VlqCodec.INSTANCE.write(writer, SNOWFLAKE);

        BinaryReader reader = new BinaryReader(writer.toByteArray());
        assertThrows(IllegalStateException.class, () -> VlqCodec.INSTANCE.readInt(reader));
    }

    @Test
    void keepsAskingForIntOutOfAWideVariantSafe() {
        VariantValue wide = Variants.of(SNOWFLAKE);

        assertTrue(Variants.asInt(wide).isEmpty(), "a snowflake is not an int");
        assertEquals(42, Variants.asInt(Variants.of(42)).orElseThrow());
    }

    @Test
    void stillRefusesNegativeUnsignedValues() {
        BinaryWriter writer = new BinaryWriter();

        assertThrows(IllegalArgumentException.class, () -> VlqUnsignedCodec.INSTANCE.write(writer, -1L));
    }
}
