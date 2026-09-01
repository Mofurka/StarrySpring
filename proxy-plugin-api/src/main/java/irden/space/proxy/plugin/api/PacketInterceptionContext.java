package irden.space.proxy.plugin.api;

import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketEnvelope;
import irden.space.proxy.protocol.packet.PacketEnvelopes;

import java.util.Objects;
import java.util.function.Supplier;


public final class PacketInterceptionContext {

    private final PluginSessionContext session;
    private final PacketEnvelope envelope;
    private final PacketDirection direction;
    private final Supplier<Object> parsedPayloadSupplier;

    private volatile boolean parsedPayloadResolved;
    private volatile Object parsedPayload;

    public PacketInterceptionContext(
            PluginSessionContext session,
            PacketEnvelope envelope,
            Object parsedPayload,
            PacketDirection direction
    ) {
        this.session = session;
        this.envelope = envelope;
        this.direction = direction;
        this.parsedPayloadSupplier = null;
        this.parsedPayload = parsedPayload;
        this.parsedPayloadResolved = true;
    }

    private PacketInterceptionContext(
            PluginSessionContext session,
            PacketEnvelope envelope,
            Supplier<Object> parsedPayloadSupplier,
            PacketDirection direction,
            boolean lazy
    ) {
        this.session = session;
        this.envelope = envelope;
        this.direction = direction;
        this.parsedPayloadSupplier = Objects.requireNonNull(parsedPayloadSupplier, "parsedPayloadSupplier");
        this.parsedPayloadResolved = !lazy;
    }

    /**
     * Контекст с отложенным разбором payload. {@code parsedPayloadSupplier} будет вызван
     * не более одного раза - при первом обращении к {@link #parsedPayload()}.
     */
    public static PacketInterceptionContext lazy(
            PluginSessionContext session,
            PacketEnvelope envelope,
            Supplier<Object> parsedPayloadSupplier,
            PacketDirection direction
    ) {
        return new PacketInterceptionContext(session, envelope, parsedPayloadSupplier, direction, true);
    }

    public PluginSessionContext session() {
        return session;
    }

    public PacketEnvelope envelope() {
        return envelope;
    }

    public PacketDirection direction() {
        return direction;
    }

    public Object parsedPayload() {
        if (parsedPayloadResolved) {
            return parsedPayload;
        }

        synchronized (this) {
            if (!parsedPayloadResolved) {
                parsedPayload = parsedPayloadSupplier.get();
                parsedPayloadResolved = true;
            }
        }

        return parsedPayload;
    }

    public <T> T parsedPayload(Class<T> clazz) {
        return clazz.cast(parsedPayload());
    }

    public PacketEnvelope envelopeWithPayload(Object payload) {
        return PacketEnvelopes.rewrite(
                envelope,
                payload,
                session.openProtocolVersion()
        );
    }

    public PacketEnvelope envelopeWithRawPayload(byte[] payload) {
        return PacketEnvelopes.rewriteRawPayload(envelope, payload);
    }

    public PacketDecision replaceWithPayload(Object payload) {
        return PacketDecision.replace(envelopeWithPayload(payload));
    }

    public PacketDecision replaceWithRawPayload(byte[] payload) {
        return PacketDecision.replace(envelopeWithRawPayload(payload));
    }


    @Override
    public String toString() {
        return "PacketInterceptionContext{"
                + "sessionId=" + (session == null ? null : session.sessionId())
                + ", packetType=" + (envelope == null ? null : envelope.packetType())
                + ", direction=" + direction
                + ", parsedPayloadResolved=" + parsedPayloadResolved
                + '}';
    }
}
