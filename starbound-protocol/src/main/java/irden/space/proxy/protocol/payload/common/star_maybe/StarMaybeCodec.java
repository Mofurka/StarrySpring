package irden.space.proxy.protocol.payload.common.star_maybe;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;

import java.util.Optional;

public class StarMaybeCodec<T> implements BinaryCodec<Optional<T>> {

    private final BinaryCodec<T> innerCodec;

    public StarMaybeCodec(BinaryCodec<T> innerCodec) {
        this.innerCodec = innerCodec;
    }

    @Override
    public Optional<T> read(BinaryReader reader) {
        boolean isPresent = reader.readBoolean();
        if (isPresent) {
            return Optional.of(innerCodec.read(reader));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void write(BinaryWriter writer, Optional<T> value) {
        if (value.isPresent()) {
            writer.writeBoolean(true);
            innerCodec.write(writer, value.get());
        } else {
            writer.writeBoolean(false);
        }
    }
}
