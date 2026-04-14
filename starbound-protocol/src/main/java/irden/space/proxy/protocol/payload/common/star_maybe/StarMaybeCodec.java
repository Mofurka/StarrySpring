package irden.space.proxy.protocol.payload.common.star_maybe;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

import java.util.Optional;

public class StarMaybeCodec<T> implements BinaryCodec<StarMaybe<T>> {
    private final BinaryCodec<T> valueCodec;

    public StarMaybeCodec(BinaryCodec<T> valueCodec) {
        this.valueCodec = valueCodec;
    }

    @Override
    public StarMaybe<T> read(BinaryReader reader) {
        boolean hasValue = reader.readBoolean();
        if (hasValue) {
            return new StarMaybe<>(Optional.of(valueCodec.read(reader)));
        } else {
            return new StarMaybe<>(Optional.empty());
        }
    }

    @Override
    public void write(BinaryWriter writer, StarMaybe<T> value) {
        if (value.value().isPresent()) {
            writer.writeBoolean(true);
            valueCodec.write(writer, value.value().get());
        } else {
            writer.writeBoolean(false);
        }
    }
}
