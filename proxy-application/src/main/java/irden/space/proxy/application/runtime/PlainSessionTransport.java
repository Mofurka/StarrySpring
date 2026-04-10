package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PlainSessionTransport implements SessionTransport {

    private final RuntimePacketReader packetReader;
    private final RuntimePacketWriter packetWriter;

    public PlainSessionTransport(RuntimePacketReader packetReader, RuntimePacketWriter packetWriter) {
        this.packetReader = packetReader;
        this.packetWriter = packetWriter;
    }

    @Override
    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        return packetReader.read(inputStream, direction);
    }

    @Override
    public void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException {
        packetWriter.write(outputStream, envelope);
    }

    @Override
    public SessionTransportMode mode() {
        return SessionTransportMode.PLAIN;
    }
}
