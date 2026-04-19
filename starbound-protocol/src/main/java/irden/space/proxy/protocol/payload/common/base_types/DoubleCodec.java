package irden.space.proxy.protocol.payload.common.base_types;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

public enum DoubleCodec implements BinaryCodec<Double> {
    INSTANCE;


    @Override
    public Double read(BinaryReader reader) {
        return reader.readDouble64BE();
    }

    @Override
    public void write(BinaryWriter writer, Double value) {
        writer.writeDouble64BE(value);
    }
}
