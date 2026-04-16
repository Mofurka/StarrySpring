package irden.space.proxy.protocol.payload.packet.client_connect;

import irden.space.proxy.protocol.codec.BinaryCodec;
import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUCodec;

import java.util.ArrayList;
import java.util.List;

public enum WorldChunksCodec implements BinaryCodec<WorldChunks> {
    INSTANCE;

    @Override
    public WorldChunks read(BinaryReader reader) {
        int length = VlqUCodec.INSTANCE.read(reader);
        List<WorldChunks.Chunk> content = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            int firstLength = VlqUCodec.INSTANCE.read(reader);
            byte[] first = reader.readBytes(firstLength);
            byte separator = (byte) reader.readUnsignedByte();
            int secondLength = VlqUCodec.INSTANCE.read(reader);
            byte[] second = reader.readBytes(secondLength);
            content.add(new WorldChunks.Chunk(first, separator, second));
        }
        return new WorldChunks(length, content);
    }

    @Override
    public void write(BinaryWriter writer, WorldChunks value) {
        VlqUCodec.INSTANCE.write(writer, value.length());
        for (WorldChunks.Chunk chunk : value.content()) {
            VlqUCodec.INSTANCE.write(writer, chunk.first().length);
            writer.writeBytes(chunk.first());
            writer.writeByte(Byte.toUnsignedInt(chunk.separator()));
            VlqUCodec.INSTANCE.write(writer, chunk.second().length);
            writer.writeBytes(chunk.second());
        }
    }
}

