package irden.space.proxy.plugin.command_handler.entity_message;

import irden.space.proxy.plugin.api.DropPacketDecision;
import irden.space.proxy.plugin.api.ForwardPacketDecision;
import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.command_handler.entity_message.RecordingSessionContext.Sent;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.payload.packet.entity_message_response.EntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.FailedEntityMessageResponse;
import irden.space.proxy.protocol.payload.packet.entity_message_response.SuccessEntityMessageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EntityMessageInterceptorTest {

    private static final String PLUGIN_ID = "entity-message-interceptor-test";

    private final EntityMessageInterceptor interceptor = new EntityMessageInterceptor(new EntityMessageService());
    private final RecordingSessionContext session = new RecordingSessionContext();
    private final Handlers handlers = new Handlers();

    @BeforeEach
    void registerHandlers() throws NoSuchMethodException {
        register("slow");
        register("failing");
    }

    @AfterEach
    void cleanUp() {
        EntityMessageRegistry.global().unregisterByPluginId(PLUGIN_ID);
        interceptor.destroy();
    }

    @Test
    void slowHandlerDoesNotBlockPacketThread() throws InterruptedException {
        StarUuid uuid = StarUuid.fromJavaUuid(UUID.randomUUID());

        PacketDecision decision = interceptor.onEntityMessage(entityMessage("test:slow", uuid, PacketDirection.TO_SERVER));

        assertInstanceOf(DropPacketDecision.class, decision);
        assertTrue(handlers.started.await(5, TimeUnit.SECONDS), "handler should run in background");
        assertTrue(session.sent.isEmpty(), "response must wait for the handler");

        handlers.release.countDown();

        Sent sent = session.awaitSent();
        assertEquals(PacketDirection.TO_CLIENT, sent.direction());
        assertEquals(PacketType.ENTITY_MESSAGE_RESPONSE, sent.type());
        EntityMessageResponse response = assertInstanceOf(EntityMessageResponse.class, sent.payload());
        assertEquals(uuid, response.uuid());
        var success = assertInstanceOf(SuccessEntityMessageResponse.class, response.response());
        assertEquals("done", Variants.asString(success.response()).orElseThrow());
    }

    @Test
    void responseGoesBackTowardsSender() throws InterruptedException {
        handlers.release.countDown();

        interceptor.onEntityMessage(entityMessage("test:slow", StarUuid.fromJavaUuid(UUID.randomUUID()), PacketDirection.TO_CLIENT));

        assertEquals(PacketDirection.TO_SERVER, session.awaitSent().direction());
    }

    @Test
    void failingHandlerRespondsWithFailure() throws InterruptedException {
        interceptor.onEntityMessage(entityMessage("test:failing", StarUuid.fromJavaUuid(UUID.randomUUID()), PacketDirection.TO_SERVER));

        EntityMessageResponse response = (EntityMessageResponse) session.awaitSent().payload();
        var failure = assertInstanceOf(FailedEntityMessageResponse.class, response.response());
        assertEquals("boom", failure.error());
    }

    @Test
    void stoppedInterceptorAnswersImmediately() {
        interceptor.destroy();

        PacketDecision decision = interceptor.onEntityMessage(
                entityMessage("test:slow", StarUuid.fromJavaUuid(UUID.randomUUID()), PacketDirection.TO_SERVER));

        assertInstanceOf(DropPacketDecision.class, decision);
        Sent sent = session.sent.poll();
        assertNotNull(sent, "stopped interceptor must still answer, otherwise the caller's promise hangs");
        EntityMessageResponse response = (EntityMessageResponse) sent.payload();
        assertInstanceOf(FailedEntityMessageResponse.class, response.response());
    }

    @Test
    void unknownMessageIsForwarded() {
        PacketDecision decision = interceptor.onEntityMessage(
                entityMessage("test:unknown", StarUuid.fromJavaUuid(UUID.randomUUID()), PacketDirection.TO_SERVER));

        assertInstanceOf(ForwardPacketDecision.class, decision);
        assertTrue(session.sent.isEmpty());
    }

    private void register(String name) throws NoSuchMethodException {
        var method = Handlers.class.getMethod(name, EntityMessageContext.class);
        EntityMessageRegistry.global().register(
                PLUGIN_ID, handlers, method, method.getAnnotation(EntityMessageHandler.class));
    }

    private PacketInterceptionContext entityMessage(String message, StarUuid uuid, PacketDirection direction) {
        var payload = new EntityMessage(new EntityIdTarget(1), message, new VariantValue[0], uuid, 1);
        return new PacketInterceptionContext(session, null, payload, direction);
    }

    public static class Handlers {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @EntityMessageHandler("test:slow")
        public VariantValue slow(EntityMessageContext context) throws InterruptedException {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test never released the handler");
            }
            return Variants.of("done");
        }

        @EntityMessageHandler("test:failing")
        public VariantValue failing(EntityMessageContext context) {
            throw new IllegalStateException("boom");
        }
    }

}
