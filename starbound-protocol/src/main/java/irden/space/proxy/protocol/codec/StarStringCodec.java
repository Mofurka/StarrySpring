package irden.space.proxy.protocol.codec;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public enum StarStringCodec implements BinaryCodec<String> {
    INSTANCE;

    @Override
    public String read(BinaryReader reader) {
        byte[] data = StarByteArrayCodec.INSTANCE.read(reader);
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public void write(BinaryWriter writer, String value) {
        StarByteArrayCodec.INSTANCE.write(writer, value.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<String> readNullable(BinaryReader reader) {
        if (!reader.readBoolean()) {
            return Optional.empty();
        }
        return Optional.of(read(reader));
    }

    public void writeNullable(BinaryWriter writer, Optional<String> value) {
        if (value.isEmpty()) {
            writer.writeBoolean(false);
        } else {
            writer.writeBoolean(true);
            write(writer, value.get());
        }
    }
}