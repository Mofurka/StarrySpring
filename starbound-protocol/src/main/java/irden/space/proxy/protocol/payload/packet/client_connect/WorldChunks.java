package irden.space.proxy.protocol.payload.packet.client_connect;

import java.util.List;

public record WorldChunks(
        int length,
        List<Chunk> content
) {

    public WorldChunks {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (length != content.size()) {
            throw new IllegalArgumentException("length must match content size");
        }
    }

    public record Chunk(
            byte[] first,
            byte separator,
            byte[] second
    ) {
        public Chunk {
            if (first == null) {
                throw new IllegalArgumentException("first must not be null");
            }
            if (second == null) {
                throw new IllegalArgumentException("second must not be null");
            }
        }
    }
}
