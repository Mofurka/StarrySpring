package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class RecordingSessionContext implements PluginSessionContext {

    final BlockingQueue<Sent> sent = new LinkedBlockingQueue<>();
    private final String sessionId;
    volatile CountDownLatch gate;
    volatile RuntimeException failure;

    RecordingSessionContext() {
        this("test-session");
    }

    RecordingSessionContext(String sessionId) {
        this.sessionId = sessionId;
    }

    Sent awaitSent() throws InterruptedException {
        Sent next = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(next, "nothing was sent");
        return next;
    }

    @Override
    public void send(PacketDirection direction, PacketType packetType, Object payload) {
        CountDownLatch currentGate = gate;
        if (currentGate != null) {
            try {
                if (!currentGate.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test never opened the gate");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
        sent.add(new Sent(direction, packetType, payload));
    }

    @Override
    public String sessionId() {
        return sessionId;
    }

    @Override
    public String clientIp() {
        return "127.0.0.1";
    }

    @Override
    public Map<String, Object> attributes() {
        return Map.of();
    }

    @Override
    public boolean clientZstdEnabled() {
        return false;
    }

    @Override
    public boolean upstreamZstdEnabled() {
        return false;
    }

    record Sent(PacketDirection direction, PacketType type, Object payload) {
    }
}
