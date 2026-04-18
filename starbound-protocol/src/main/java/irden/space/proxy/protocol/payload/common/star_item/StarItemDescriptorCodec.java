package irden.space.proxy.protocol.payload.common.star_item;

import irden.space.proxy.protocol.codec.*;
import irden.space.proxy.protocol.codec.variant.VariantValue;

public enum StarItemDescriptorCodec implements BinaryCodec<StarItemDescriptor> {
    INSTANCE;
    @Override
    public StarItemDescriptor read(BinaryReader reader) {
        String name = StarStringCodec.INSTANCE.read(reader);
        int count = VlqUCodec.INSTANCE.read(reader);
        VariantValue parameters = VariantCodec.INSTANCE.read(reader);
        return new StarItemDescriptor(name, count, parameters);
    }

    @Override
    public void write(BinaryWriter writer, StarItemDescriptor value) {
        StarStringCodec.INSTANCE.write(writer, value.name());
        VlqUCodec.INSTANCE.write(writer, value.count());
        VariantCodec.INSTANCE.write(writer, value.parameters());
    }
}
