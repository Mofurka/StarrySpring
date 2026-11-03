package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.command_handler.entity_message.RecordingSessionContext.Sent;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.payload.packet.entity_message_response.FailedEntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.SuccessEntityMessageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EntityMessageServiceTest {

    private final EntityMessageService service = new EntityMessageService();

    private static EntityMessage packet(Sent sent) {
        return (EntityMessage) sent.payload();
    }

    @AfterEach
    void stop() {
        service.destroy();
    }

    @Test
    void sendDoesNotWaitForSlowClient() throws InterruptedException {
        var session = new RecordingSessionContext();
        session.gate = new CountDownLatch(1);

        CompletableFuture<VariantValue> future = assertTimeoutPreemptively(
                Duration.ofSeconds(1), () -> send(session, "update"));

        assertFalse(future.isDone());
        session.gate.countDown();
        assertEquals("update", packet(session.awaitSent()).message());
    }

    @Test
    void slowClientDoesNotDelayOtherSessions() throws InterruptedException {
        var slow = new RecordingSessionContext("slow");
        slow.gate = new CountDownLatch(1);
        var fast = new RecordingSessionContext("fast");

        send(slow, "update");
        send(fast, "update");

        assertEquals("update", packet(fast.awaitSent()).message());
        assertTrue(slow.sent.isEmpty());

        slow.gate.countDown();
        slow.awaitSent();
    }

    @Test
    void messagesToOneSessionKeepOrder() throws InterruptedException {
        var session = new RecordingSessionContext();

        for (int i = 0; i < 200; i++) {
            send(session, "message-" + i);
        }

        for (int i = 0; i < 200; i++) {
            assertEquals("message-" + i, packet(session.awaitSent()).message());
        }
    }

    @Test
    void callbacksDoNotRunOnRespondingThread() throws Exception {
        var session = new RecordingSessionContext();
        CompletableFuture<Thread> callbackThread = new CompletableFuture<>();
        send(session, "ping").thenRun(() -> callbackThread.complete(Thread.currentThread()));
        StarUuid uuid = packet(session.awaitSent()).uuid();

        assertTrue(service.complete(uuid, new SuccessEntityMessageResponse(Variants.of("pong"))));

        assertNotSame(Thread.currentThread(), callbackThread.get(5, TimeUnit.SECONDS),
                "callbacks must not run on the packet thread that delivered the response");
    }

    @Test
    void failedResponseFailsFuture() throws InterruptedException {
        var session = new RecordingSessionContext();
        CompletableFuture<VariantValue> future = send(session, "ping");
        StarUuid uuid = packet(session.awaitSent()).uuid();

        service.complete(uuid, new FailedEntityMessageResponse("not handled"));

        ExecutionException failure = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertInstanceOf(EntityMessageFailedException.class, failure.getCause());
    }

    @Test
    void writeFailureFailsFuture() {
        var session = new RecordingSessionContext();
        session.failure = new IllegalStateException("socket closed");

        CompletableFuture<VariantValue> future = send(session, "update");

        ExecutionException failure = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
        assertSame(session.failure, failure.getCause());
    }

    @Test
    void queuedMessageIsDroppedAfterTimeout() throws InterruptedException {
        var session = new RecordingSessionContext();
        session.gate = new CountDownLatch(1);
        send(session, "stuck");

        CompletableFuture<VariantValue> expired = service.send(
                session, new EntityIdTarget(1), "expired", new VariantValue[0], 0, Duration.ofMillis(50));

        ExecutionException failure = assertThrows(ExecutionException.class, () -> expired.get(5, TimeUnit.SECONDS));
        assertInstanceOf(EntityMessageTimeoutException.class, failure.getCause());

        session.gate.countDown();
        assertEquals("stuck", packet(session.awaitSent()).message());
        assertNull(session.sent.poll(200, TimeUnit.MILLISECONDS), "expired message must not be written");
    }

    @Test
    void closedSessionFailsPendingAndSkipsQueued() throws InterruptedException {
        var session = new RecordingSessionContext();
        session.gate = new CountDownLatch(1);
        send(session, "stuck");
        CompletableFuture<VariantValue> queued = send(session, "queued");

        service.purgeSession(session.sessionId());

        ExecutionException failure = assertThrows(ExecutionException.class, () -> queued.get(5, TimeUnit.SECONDS));
        assertInstanceOf(EntityMessageSessionClosedException.class, failure.getCause());

        session.gate.countDown();
        assertEquals("stuck", packet(session.awaitSent()).message());
        assertNull(session.sent.poll(200, TimeUnit.MILLISECONDS), "queued message of a closed session must not be written");
    }

    @Test
    void stoppedServiceFailsInsteadOfHanging() {
        service.destroy();

        CompletableFuture<VariantValue> future = send(new RecordingSessionContext(), "update");

        assertTrue(future.isCompletedExceptionally());
    }

    private CompletableFuture<VariantValue> send(RecordingSessionContext session, String message) {
        return service.sendToEntity(session, 1, message);
    }
}
