package irden.space.proxy.application.runtime;

import irden.space.proxy.application.port.out.SessionRegistry;
import irden.space.proxy.domain.session.ProxySession;
import irden.space.proxy.domain.session.ProxySessionId;
import irden.space.proxy.plugin.api.ForwardPacketDecision;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketForwarderTest {

    @Test
    void enablesZstdReadImmediatelyAndDelaysZstdWrite() throws Exception {
        ProxySession session = new ProxySession(ProxySessionId.generate(), "127.0.0.1");
        TrackingTransport clientTransport = new TrackingTransport();
        TrackingTransport upstreamTransport = new TrackingTransport();

        try (Socket clientSocket = new Socket();
             Socket upstreamSocket = new Socket()) {
            ProxySessionRuntimeContext context = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    clientTransport,
                    upstreamTransport
            );

            PacketForwarder forwarder = new PacketForwarder(
                    new ByteArrayInputStream(new byte[0]),
                    new ByteArrayOutputStream(),
                    null,
                    PacketDirection.TO_CLIENT,
                    context,
                    upstreamTransport,
                    null,
                    ignored -> ForwardPacketDecision.INSTANCE
            );

            Method method = PacketForwarder.class.getDeclaredMethod("switchSessionToZstd");
            method.setAccessible(true);

            long startedAt = System.nanoTime();
            method.invoke(forwarder);

            assertTrue(clientTransport.firstReadToggleDelayMillisFrom(startedAt) < 20,
                    "client-side read transport was not switched to ZSTD immediately");
            assertTrue(upstreamTransport.firstReadToggleDelayMillisFrom(startedAt) < 20,
                    "upstream-side read transport was not switched to ZSTD immediately");
            assertTrue(clientTransport.firstWriteToggleDelayMillisFrom(startedAt) >= 20,
                    "client-side write transport switched to ZSTD before grace period elapsed");
            assertTrue(upstreamTransport.firstWriteToggleDelayMillisFrom(startedAt) >= 20,
                    "upstream-side write transport switched to ZSTD before grace period elapsed");
            assertTrue(clientTransport.isZstdReadEnabled());
            assertTrue(clientTransport.isZstdWriteEnabled());
            assertTrue(upstreamTransport.isZstdReadEnabled());
            assertTrue(upstreamTransport.isZstdWriteEnabled());
        }
    }

    @Test
    void switchesToZstdBeforeWritingNegotiatedProtocolResponse() throws Exception {
        ProxySession session = new ProxySession(ProxySessionId.generate(), "127.0.0.1");
        RuntimePacketInspector packetInspector = new RuntimePacketInspector(null) {
            @Override
            public PacketInspectionResult inspect(PacketEnvelope envelope, PacketDirection direction) {
                return new PacketInspectionResult("protocol-response", true, false);
            }
        };

        try (Socket clientSocket = new Socket();
             Socket upstreamSocket = new Socket()) {
            RecordingTransport clientTransport = new RecordingTransport(clientSocket, upstreamSocket);
            RecordingTransport upstreamTransport = new RecordingTransport(clientSocket, upstreamSocket);

            ProxySessionRuntimeContext context = new ProxySessionRuntimeContext(
                    session,
                    clientSocket,
                    upstreamSocket,
                    clientTransport,
                    upstreamTransport
            );

            PacketForwarder forwarder = new PacketForwarder(
                    new ByteArrayInputStream(new byte[0]),
                    new ByteArrayOutputStream(),
                    new NoOpSessionRegistry(),
                    PacketDirection.TO_CLIENT,
                    context,
                    upstreamTransport,
                    packetInspector,
                    ignored -> ForwardPacketDecision.INSTANCE
            );

            forwarder.run();

            assertTrue(upstreamTransport.wasWrittenWithZstdEnabled(),
                    "negotiated protocol response was written before ZSTD write mode was enabled");
        }
    }

    private static final class TrackingTransport extends SwitchableSessionTransport {

        private volatile long firstReadToggleAt = Long.MIN_VALUE;
        private volatile long firstWriteToggleAt = Long.MIN_VALUE;

        private TrackingTransport() {
            super(new PlainSessionTransport());
        }

        @Override
        public synchronized void enableZstdRead() {
            if (firstReadToggleAt == Long.MIN_VALUE) {
                firstReadToggleAt = System.nanoTime();
            }
            super.enableZstdRead();
        }

        @Override
        public synchronized void enableZstdWrite(int skipPackets) {
            if (firstWriteToggleAt == Long.MIN_VALUE) {
                firstWriteToggleAt = System.nanoTime();
            }
            super.enableZstdWrite(skipPackets);
        }

        private long firstReadToggleDelayMillisFrom(long startedAt) {
            return TimeUnit.NANOSECONDS.toMillis(firstReadToggleAt - startedAt);
        }

        private long firstWriteToggleDelayMillisFrom(long startedAt) {
            return TimeUnit.NANOSECONDS.toMillis(firstWriteToggleAt - startedAt);
        }
    }

    private static final class RecordingTransport extends SwitchableSessionTransport {

        private final Socket clientSocket;
        private final Socket upstreamSocket;
        private boolean readDelivered;
        private boolean writtenWithZstdEnabled;

        private RecordingTransport(Socket clientSocket, Socket upstreamSocket) {
            super(new PlainSessionTransport());
            this.clientSocket = clientSocket;
            this.upstreamSocket = upstreamSocket;
        }

        @Override
        public PacketEnvelope read(java.io.InputStream inputStream, PacketDirection direction) throws IOException {
            if (!readDelivered) {
                readDelivered = true;
                return new PacketEnvelope(
                        1,
                        PacketType.PROTOCOL_RESPONSE,
                        0,
                        false,
                        new byte[0],
                        new byte[]{1, 0},
                        direction
                );
            }

            try {
                clientSocket.close();
                upstreamSocket.close();
            } catch (IOException ignored) {
            }
            throw new SocketException("test completed");
        }

        @Override
        public void write(java.io.OutputStream outputStream, PacketEnvelope envelope) {
            writtenWithZstdEnabled = isZstdWriteEnabled();
        }

        private boolean wasWrittenWithZstdEnabled() {
            return writtenWithZstdEnabled;
        }
    }

    private static final class NoOpSessionRegistry implements SessionRegistry {

        @Override
        public void add(ProxySession session) {
        }

        @Override
        public void remove(ProxySessionId sessionId) {
        }

        @Override
        public Optional<ProxySession> getById(ProxySessionId sessionId) {
            return Optional.empty();
        }

        @Override
        public Collection<ProxySession> getAllSessions() {
            return List.of();
        }
    }
}

