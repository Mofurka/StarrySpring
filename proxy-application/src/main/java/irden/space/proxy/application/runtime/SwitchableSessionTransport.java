package irden.space.proxy.application.runtime;


import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

public class SwitchableSessionTransport implements SessionTransport {

    private final AtomicReference<SessionTransport> delegate;

    public SwitchableSessionTransport(SessionTransport initialTransport) {
        this.delegate = new AtomicReference<>(initialTransport);
    }

    @Override
    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        return delegate.get().read(inputStream, direction);
    }

    @Override
    public void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException {
        delegate.get().write(outputStream, envelope);
    }

    @Override
    public SessionTransportMode mode() {
        return delegate.get().mode();
    }

    public void switchToZstd() {
        switchTo(new ZstdSessionTransport());
    }

    public void switchTo(SessionTransport newTransport) {
        delegate.set(newTransport);
    }

    public boolean isPlain() {
        return mode() == SessionTransportMode.PLAIN;
    }

    public boolean isZstd() {
        return mode() == SessionTransportMode.ZSTD;
    }
}
