package irden.space.proxy.application.runtime;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import irden.space.proxy.domain.session.SessionTransportMode;
import irden.space.proxy.protocol.packet.PacketEnvelope;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ZstdSessionTransportCodec implements SessionTransportCodec {

    @Override
    public SessionTransportMode mode() {
        return SessionTransportMode.ZSTD;
    }

    @Override
    public InputStream wrapRead(InputStream inputStream) throws IOException {
        return new ZstdInputStream(inputStream);
    }

    @Override
    public byte[] encode(PacketEnvelope envelope) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZstdOutputStream zstdOutputStream = new ZstdOutputStream(outputStream)) {
            zstdOutputStream.write(envelope.originalData());
        }
        return outputStream.toByteArray();
    }
}

