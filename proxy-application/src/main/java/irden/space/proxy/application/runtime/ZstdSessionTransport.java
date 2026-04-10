package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ZstdSessionTransport implements SessionTransport {

    @Override
    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        throw new UnsupportedOperationException("ZSTD transport is not implemented yet");
    }

    @Override
    public void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException {
        throw new UnsupportedOperationException("ZSTD transport is not implemented yet");
    }

    @Override
    public SessionTransportMode mode() {
        return SessionTransportMode.ZSTD;
    }
}