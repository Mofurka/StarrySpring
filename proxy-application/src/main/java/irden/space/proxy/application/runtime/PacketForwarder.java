package irden.space.proxy.application.runtime;
import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class PacketForwarder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PacketForwarder.class);

    private final ProxySession session;
    private final InputStream source;
    private final OutputStream target;
    private final Socket clientSocket;
    private final Socket upstreamSocket;
    private final SessionRegistry sessionRegistry;
    private final RuntimePacketReader packetReader;
    private final RuntimePacketWriter packetWriter;
    private final PacketDirection packetDirection;

    public PacketForwarder(
            ProxySession session,
            InputStream source,
            OutputStream target,
            Socket clientSocket,
            Socket upstreamSocket,
            SessionRegistry sessionRegistry,
            RuntimePacketReader packetReader,
            RuntimePacketWriter packetWriter,
            PacketDirection packetDirection
    ) {
        this.session = session;
        this.source = source;
        this.target = target;
        this.clientSocket = clientSocket;
        this.upstreamSocket = upstreamSocket;
        this.sessionRegistry = sessionRegistry;
        this.packetReader = packetReader;
        this.packetWriter = packetWriter;
        this.packetDirection = packetDirection;
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed() && !upstreamSocket.isClosed()) {
                PacketEnvelope envelope = packetReader.read(source, packetDirection);

                log.debug(
                        "[{}] session={} rawType={} type={} size={} compressed={}",
                        packetDirection,
                        session.getId(),
                        envelope.rawPacketTypeId(),
                        envelope.packetType(),
                        envelope.payloadSize(),
                        envelope.compressed()
                );

                packetWriter.write(target, envelope);
            }
        } catch (SocketException e) {
            log.info("[{}] socket closed for session {}", packetDirection, session.getId());
        } catch (Exception e) {
            log.warn("[{}] forwarding stopped for session {}: {}", packetDirection, session.getId(), e.getMessage(), e);
        } finally {
            closeSession();
        }
    }

    private void closeSession() {
        synchronized (session) {
            if (session.getState().name().equals("DISCONNECTED")) {
                return;
            }

            try {
                session.markDisconnecting();
            } catch (Exception ignored) {
            }

            try {
                clientSocket.close();
            } catch (Exception ignored) {
            }

            try {
                upstreamSocket.close();
            } catch (Exception ignored) {
            }

            try {
                session.markDisconnected();
            } catch (Exception ignored) {
            }

            sessionRegistry.remove(session.getId());

            log.info("Session {} closed and removed", session.getId());
        }
    }
}
