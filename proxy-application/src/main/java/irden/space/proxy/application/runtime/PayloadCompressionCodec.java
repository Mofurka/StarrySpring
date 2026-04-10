package irden.space.proxy.application.runtime;


import java.io.IOException;

public interface PayloadCompressionCodec {

    byte[] decompress(byte[] payload) throws IOException;
}