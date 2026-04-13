package irden.space.proxy.protocol.payload.common.maybe;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

import java.util.Optional;

public class MaybeCodec<T> implements BinaryCodec<Maybe<T>> {
    private final BinaryCodec<T> valueCodec;

    public MaybeCodec(BinaryCodec<T> valueCodec) {
        this.valueCodec = valueCodec;
    }

    @Override
    public Maybe<T> read(BinaryReader reader) {
        boolean hasValue = reader.readBoolean();
        if (hasValue) {
            return new Maybe<>(Optional.of(valueCodec.read(reader)));
        } else {
            return new Maybe<>(Optional.empty());
        }
    }

    @Override
    public void write(BinaryWriter writer, Maybe<T> value) {
        if (value.value().isPresent()) {
            writer.writeBoolean(true);
            valueCodec.write(writer, value.value().get());
        } else {
            writer.writeBoolean(false);
        }
    }
}
