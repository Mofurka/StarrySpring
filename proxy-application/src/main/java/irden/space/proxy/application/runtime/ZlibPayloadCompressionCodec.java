package irden.space.proxy.application.runtime;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

public class ZlibPayloadCompressionCodec implements PayloadCompressionCodec {

    @Override
    public byte[] decompress(byte[] payload) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(payload);
             InflaterInputStream inflaterInputStream = new InflaterInputStream(byteArrayInputStream)) {
            return inflaterInputStream.readAllBytes();
        } catch (IOException e) {
            throw new IOException("Failed to decompress zlib payload", e);
        }
    }
}