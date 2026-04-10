package irden.space.proxy.application.runtime;

import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.OutputStream;

public class RuntimePacketWriter {

    public void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException {
        outputStream.write(envelope.originalData());
        outputStream.flush();
    }
}