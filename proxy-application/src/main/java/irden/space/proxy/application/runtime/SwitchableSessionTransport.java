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

    private volatile SessionTransportMode readMode;
    private volatile SessionTransportMode writeMode;
    private int writeSkipPackets;

    public SwitchableSessionTransport(SessionTransport initialTransport) {
        this(DEFAULT_CODECS, initialTransport.mode());
    }

    public SwitchableSessionTransport(SessionTransportMode initialMode) {
        this(DEFAULT_CODECS, initialMode);
    }

    SwitchableSessionTransport(Map<SessionTransportMode, SessionTransportCodec> codecs, SessionTransportMode initialMode) {
        this.packetReader = new RuntimePacketReader(new ZlibPayloadCompressionCodec());
        this.packetWriter = new RuntimePacketWriter();
        this.codecs = Map.copyOf(codecs);
        this.readMode = requireSupportedMode(initialMode);
        this.writeMode = requireSupportedMode(initialMode);
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

        return readMode;
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
        this.readMode = requireSupportedMode(mode);
    }

    public synchronized void enableWriteMode(SessionTransportMode mode, int skipPackets) {
        this.writeMode = requireSupportedMode(mode);
        this.writeSkipPackets = Math.max(skipPackets, 0);
    }

    public synchronized boolean isReadModeEnabled(SessionTransportMode mode) {
        return readMode == mode;
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

        if (readMode == SessionTransportMode.PLAIN) {
            return readSource;
        }

        if (wrappedReadSource == null) {
            wrappedReadSource = resolveCodec(readMode).wrapRead(readSource);
            wrappedReadMode = readMode;
        } else if (wrappedReadMode != readMode) {
            throw new IOException("Transport read mode cannot change after wrapped stream initialization");
        }

        return wrappedReadSource;
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
}
