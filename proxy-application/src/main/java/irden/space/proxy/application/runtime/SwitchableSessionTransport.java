package irden.space.proxy.application.runtime;

import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public class SwitchableSessionTransport implements SessionTransport {

    private static final Map<SessionTransportMode, SessionTransportCodec> DEFAULT_CODECS = defaultCodecs();

    private final RuntimePacketReader packetReader;
    private final RuntimePacketWriter packetWriter;
    private final Map<SessionTransportMode, SessionTransportCodec> codecs;

    private InputStream readSource;
    private InputStream wrappedReadSource;
    private SessionTransportMode wrappedReadMode;
    private OutputStream writeTarget;


    private volatile SessionTransportMode requestedReadMode;
    private volatile SessionTransportMode appliedReadMode;
    private volatile SessionTransportMode writeMode;
    private int writeSkipPackets;

    public SwitchableSessionTransport(SessionTransport initialTransport) {
        this(DEFAULT_CODECS, initialTransport.mode(), RuntimePacketReader.DEFAULT_MAX_PAYLOAD_SIZE_BYTES);
    }

    public SwitchableSessionTransport(SessionTransportMode initialMode) {
        this(DEFAULT_CODECS, initialMode, RuntimePacketReader.DEFAULT_MAX_PAYLOAD_SIZE_BYTES);
    }

    public SwitchableSessionTransport(SessionTransportMode initialMode, int maxPayloadSizeBytes) {
        this(DEFAULT_CODECS, initialMode, maxPayloadSizeBytes);
    }

    SwitchableSessionTransport(
            Map<SessionTransportMode, SessionTransportCodec> codecs,
            SessionTransportMode initialMode,
            int maxPayloadSizeBytes
    ) {
        this.packetReader = new RuntimePacketReader(new ZlibPayloadCompressionCodec(), maxPayloadSizeBytes);
        this.packetWriter = new RuntimePacketWriter();
        this.codecs = Map.copyOf(codecs);
        this.requestedReadMode = requireSupportedMode(initialMode);
        this.appliedReadMode = this.requestedReadMode;
        this.writeMode = requireSupportedMode(initialMode);
    }

    private static Map<SessionTransportMode, SessionTransportCodec> defaultCodecs() {
        Map<SessionTransportMode, SessionTransportCodec> codecs = new EnumMap<>(SessionTransportMode.class);
        register(codecs, new PlainSessionTransportCodec());
        register(codecs, new ZstdSessionTransportCodec());
        return codecs;
    }

    private static void register(Map<SessionTransportMode, SessionTransportCodec> codecs,
                                 SessionTransportCodec codec) {
        codecs.put(codec.mode(), codec);
    }

    @Override
    public PacketEnvelope read(InputStream inputStream, PacketDirection direction) throws IOException {
        return packetReader.read(resolveReadSource(inputStream), direction);
    }

    @Override
    public void write(OutputStream outputStream, PacketEnvelope envelope) throws IOException {
        OutputStream resolvedTarget = bindWriteTarget(outputStream);

        if (shouldWritePlainPacket()) {
            packetWriter.write(resolvedTarget, envelope);
            return;
        }

        resolvedTarget.write(resolveCodec(writeMode).encode(envelope));
    }

    @Override
    public SessionTransportMode mode() {
        if (writeMode != SessionTransportMode.PLAIN) {
            return writeMode;
        }

        return requestedReadMode;
    }

    public boolean isZstd() {
        return mode() == SessionTransportMode.ZSTD;
    }

    public synchronized void enableZstdRead() {
        enableReadMode(SessionTransportMode.ZSTD);
    }

    public synchronized void enableZstdWrite(int skipPackets) {
        enableWriteMode(SessionTransportMode.ZSTD, skipPackets);
    }

    public synchronized boolean isZstdReadEnabled() {
        return isReadModeEnabled(SessionTransportMode.ZSTD);
    }

    public synchronized boolean isZstdWriteEnabled() {
        return isWriteModeEnabled(SessionTransportMode.ZSTD);
    }


    public synchronized void enableReadMode(SessionTransportMode mode) {
        this.requestedReadMode = requireSupportedMode(mode);
        if (readSource == null) {
            this.appliedReadMode = this.requestedReadMode;
        }
        notifyAll();
    }


    public synchronized boolean awaitReadModeApplied(SessionTransportMode mode, long timeoutMillis)
            throws InterruptedException {
        long deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (appliedReadMode != mode) {
            long remainingNanos = deadlineNanos - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }
            wait(remainingNanos / 1_000_000L + 1L);
        }
        return true;
    }

    public synchronized boolean isReadModeApplied(SessionTransportMode mode) {
        return appliedReadMode == mode;
    }

    public synchronized void enableWriteMode(SessionTransportMode mode, int skipPackets) {
        this.writeMode = requireSupportedMode(mode);
        this.writeSkipPackets = Math.max(skipPackets, 0);
    }

    public synchronized boolean isReadModeEnabled(SessionTransportMode mode) {
        return requestedReadMode == mode;
    }

    public synchronized boolean isWriteModeEnabled(SessionTransportMode mode) {
        return writeMode == mode;
    }

    private synchronized InputStream resolveReadSource(InputStream inputStream) throws IOException {
        if (readSource == null) {
            readSource = inputStream;
        } else if (readSource != inputStream) {
            throw new IOException("Transport read source cannot change within a session");
        }

        applyRequestedReadMode();

        if (appliedReadMode == SessionTransportMode.PLAIN) {
            return readSource;
        }

        if (wrappedReadSource == null) {
            wrappedReadSource = resolveCodec(appliedReadMode).wrapRead(readSource);
            wrappedReadMode = appliedReadMode;
        } else if (wrappedReadMode != appliedReadMode) {
            throw new IOException("Transport read mode cannot change after wrapped stream initialization");
        }

        return wrappedReadSource;
    }


    private void applyRequestedReadMode() throws IOException {
        if (appliedReadMode == requestedReadMode) {
            return;
        }

        if (wrappedReadSource != null) {
            throw new IOException("Transport read mode cannot change after wrapped stream initialization");
        }

        appliedReadMode = requestedReadMode;
        notifyAll();
    }

    private synchronized OutputStream bindWriteTarget(OutputStream outputStream) throws IOException {
        if (writeTarget == null) {
            writeTarget = outputStream;
        } else if (writeTarget != outputStream) {
            throw new IOException("Transport write target cannot change within a session");
        }

        return writeTarget;
    }

    private synchronized boolean shouldWritePlainPacket() {
        if (writeMode == SessionTransportMode.PLAIN) {
            return true;
        }

        if (writeSkipPackets > 0) {
            writeSkipPackets--;
            return true;
        }

        return false;
    }

    private SessionTransportMode requireSupportedMode(SessionTransportMode mode) {
        Objects.requireNonNull(mode, "Transport mode cannot be null");
        if (!codecs.containsKey(mode)) {
            throw new IllegalArgumentException("Unsupported transport mode: " + mode);
        }

        return mode;
    }

    private SessionTransportCodec resolveCodec(SessionTransportMode mode) {
        SessionTransportCodec codec = codecs.get(mode);
        if (codec == null) {
            throw new IllegalStateException("No codec registered for transport mode " + mode);
        }

        return codec;
    }
}
