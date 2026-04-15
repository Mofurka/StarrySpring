package irden.space.proxy.protocol.payload.common.star_m_variant;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public class StarMVariantCodec implements BinaryCodec<StarMVariant> {
    private final BinaryCodec<?>[] codecs;

    public StarMVariantCodec(BinaryCodec<?> ... codecs) {
        this.codecs = codecs;
    }


    @Override
    public StarMVariant read(BinaryReader reader) {
        int variant = reader.readUnsignedByte();
        if (variant == 0) {
            return new StarMVariant(0, null);
        }
        if (variant > codecs.length) {
            throw new IllegalStateException("Unknown variant: " + variant);
        }
        BinaryCodec<?> codec = codecs[variant - 1];
        Object value = codec.read(reader);
        return new StarMVariant(variant, value);
    }

    @Override
    public void write(BinaryWriter writer, StarMVariant value) {
            int variant = value.variant();
            if (variant == 0) {
                writer.writeByte(0);
                return;
            }
            if (variant > codecs.length) {
                throw new IllegalStateException("Unknown variant: " + variant);
            }
            BinaryCodec codec = codecs[variant - 1];
            writer.writeByte(variant);
            codec.write(writer, value.value());
    }
}
