package irden.space.proxy.protocol.codec;

public interface BinaryCodec<T> {

    T read(BinaryReader reader);

    void write(BinaryWriter writer, T value);
}
