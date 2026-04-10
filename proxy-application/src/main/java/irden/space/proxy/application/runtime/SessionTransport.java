package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface SessionTransport {

    PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException;

    void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException;

    SessionTransportMode mode();
}
