package irden.space.proxy.plugin.native_server_lifespan.rcon;

import irden.space.proxy.plugin.native_server_lifespan.NativeServerLifespanConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(
        value = "native-server-lifespan.rcon.enabled",
        matchIfMissing = true
)
public class StarboundRconClient {

    private static final int SERVERDATA_RESPONSE_VALUE = 0;
    private static final int SERVERDATA_EXECCOMMAND = 2;
    private static final int SERVERDATA_AUTH_RESPONSE = 2;
    private static final int SERVERDATA_AUTH = 3;

    private static final int MAX_PACKET_SIZE = 4 * 1024 * 1024;

    private final NativeServerLifespanConfig config;

    private final AtomicInteger requestIdSequence = new AtomicInteger();


    public Optional<String> executeUnchecked(String command) {
        try {
            return execute(command);
        } catch (IOException e) {
            throw new RconCommandException(
                    "Failed to execute RCON command: " + command,
                    e
            );
        }
    }

    public Optional<String> execute(String command) throws IOException {
        NativeServerLifespanConfig.Rcon rcon = config.rcon();

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(rcon.host(), rcon.port()),
                    Math.toIntExact(rcon.connectTimeout().toMillis())
            );

            socket.setSoTimeout(
                    Math.toIntExact(rcon.readTimeout().toMillis())
            );

            InputStream input = socket.getInputStream();
            OutputStream output = socket.getOutputStream();

            authenticate(input, output, rcon.password());

            int requestId = nextRequestId();

            writePacket(
                    output,
                    requestId,
                    SERVERDATA_EXECCOMMAND,
                    command
            );

            try {
                RconPacket response = readPacket(input);

                if (response.requestId() != requestId) {
                    throw new IOException(
                            "Unexpected RCON response ID: " + response.requestId()
                    );
                }

                return Optional.ofNullable(response.body());
            } catch (SocketTimeoutException | EOFException e) {
                /*
                 * Для команды stop сервер может закрыть соединение раньше,
                 * чем вернёт ответ.
                 */
                return Optional.empty();
            }
        }
    }

    public void stopServer() throws IOException {
        log.info("Sending graceful shutdown command through RCON");

        execute("stop").ifPresent(response -> {
            if (!response.isBlank()) {
                log.info("RCON response: {}", response);
            }
        });
    }

    @Deprecated(forRemoval = true)
    // actually the game does not have the restart command
    public void restartServer() throws IOException {
        log.info("Sending restart command through RCON");

        execute("restart").ifPresent(response -> {
            if (!response.isBlank()) {
                log.info("RCON response: {}", response);
            }
        });
    }

    private void authenticate(
            InputStream input,
            OutputStream output,
            String password
    ) throws IOException {
        int requestId = nextRequestId();

        writePacket(
                output,
                requestId,
                SERVERDATA_AUTH,
                password
        );

        for (int attempt = 0; attempt < 3; attempt++) {
            RconPacket response = readPacket(input);

            if (response.type() != SERVERDATA_AUTH_RESPONSE) {
                continue;
            }

            if (response.requestId() == -1) {
                throw new RconAuthenticationException(
                        "RCON authentication failed"
                );
            }

            if (response.requestId() != requestId) {
                throw new IOException(
                        "Unexpected RCON authentication response ID: "
                                + response.requestId()
                );
            }

            return;
        }

        throw new IOException(
                "RCON server did not return authentication response"
        );
    }

    private int nextRequestId() {
        return requestIdSequence.updateAndGet(current ->
                current == Integer.MAX_VALUE ? 1 : current + 1
        );
    }

    private void writePacket(
            OutputStream output,
            int requestId,
            int type,
            String body
    ) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        int payloadSize =
                Integer.BYTES
                        + Integer.BYTES
                        + bodyBytes.length
                        + 2;

        ByteBuffer buffer = ByteBuffer
                .allocate(Integer.BYTES + payloadSize)
                .order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(payloadSize);
        buffer.putInt(requestId);
        buffer.putInt(type);
        buffer.put(bodyBytes);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        output.write(buffer.array());
        output.flush();
    }

    private RconPacket readPacket(InputStream input) throws IOException {
        int payloadSize = readLittleEndianInt(input);

        if (payloadSize < 10 || payloadSize > MAX_PACKET_SIZE) {
            throw new IOException(
                    "Invalid RCON packet size: " + payloadSize
            );
        }

        byte[] payload = input.readNBytes(payloadSize);

        if (payload.length != payloadSize) {
            throw new EOFException(
                    "Unexpected end of RCON packet"
            );
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(payload)
                .order(ByteOrder.LITTLE_ENDIAN);

        int requestId = buffer.getInt();
        int type = buffer.getInt();

        int bodyStart = Integer.BYTES * 2;
        int bodyEnd = bodyStart;

        while (bodyEnd < payload.length && payload[bodyEnd] != 0) {
            bodyEnd++;
        }

        String body = new String(
                payload,
                bodyStart,
                bodyEnd - bodyStart,
                StandardCharsets.UTF_8
        );

        return new RconPacket(requestId, type, body);
    }

    private int readLittleEndianInt(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(Integer.BYTES);

        if (bytes.length != Integer.BYTES) {
            throw new EOFException(
                    "Unexpected end of RCON stream"
            );
        }

        return ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    private record RconPacket(
            int requestId,
            int type,
            String body
    ) {
    }
}