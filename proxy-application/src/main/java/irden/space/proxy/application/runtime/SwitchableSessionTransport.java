package irden.space.proxy.application.runtime;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SwitchableSessionTransport implements SessionTransport {

    private final RuntimePacketReader packetReader;
    private final RuntimePacketWriter packetWriter;

    private InputStream readSource;
    private ZstdInputStream zstdReadSource;
    private OutputStream writeTarget;

    private volatile boolean zstdReadEnabled;
    private volatile boolean zstdWriteEnabled;
    private int writeSkipPackets;

    public SwitchableSessionTransport(SessionTransport initialTransport) {
        this.packetReader = new RuntimePacketReader(new ZlibPayloadCompressionCodec());
        this.packetWriter = new RuntimePacketWriter();
        this.zstdReadEnabled = initialTransport.mode() == SessionTransportMode.ZSTD;
        this.zstdWriteEnabled = initialTransport.mode() == SessionTransportMode.ZSTD;
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

        resolvedTarget.write(compressZstdFrame(envelope.originalData()));
        resolvedTarget.flush();
    }

    @Override
    public SessionTransportMode mode() {
        return zstdReadEnabled || zstdWriteEnabled
                ? SessionTransportMode.ZSTD
                : SessionTransportMode.PLAIN;
    }

    public boolean isZstd() {
        return mode() == SessionTransportMode.ZSTD;
    }

    public synchronized void enableZstdRead() {
        this.zstdReadEnabled = true;
    }

    public synchronized void enableZstdWrite(int skipPackets) {
        this.zstdWriteEnabled = true;
        this.writeSkipPackets = Math.max(skipPackets, 0);
    }

    public synchronized boolean isZstdReadEnabled() {
        return zstdReadEnabled;
    }

    public synchronized boolean isZstdWriteEnabled() {
        return zstdWriteEnabled;
    }

    private synchronized InputStream resolveReadSource(InputStream inputStream) throws IOException {
        if (readSource == null) {
            readSource = inputStream;
        } else if (readSource != inputStream) {
            throw new IOException("Transport read source cannot change within a session");
        }

        if (!zstdReadEnabled) {
            return readSource;
        }

        if (zstdReadSource == null) {
            zstdReadSource = new ZstdInputStream(readSource);
        }

        return zstdReadSource;
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
        if (!zstdWriteEnabled) {
            return true;
        }

        if (writeSkipPackets > 0) {
            writeSkipPackets--;
            return true;
        }

        return false;
    }

    private byte[] compressZstdFrame(byte[] packetBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZstdOutputStream zstdOutputStream = new ZstdOutputStream(outputStream)) {
            zstdOutputStream.write(packetBytes);
        }
        return outputStream.toByteArray();
    }
}