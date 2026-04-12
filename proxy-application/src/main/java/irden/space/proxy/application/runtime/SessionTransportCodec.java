package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;

public interface SessionTransportCodec {

    SessionTransportMode mode();

    default InputStream wrapRead(InputStream inputStream) throws IOException {
        return inputStream;
    }

    default byte[] encode(PacketEnvelope envelope) throws IOException {
        return envelope.originalData();
    }
}

