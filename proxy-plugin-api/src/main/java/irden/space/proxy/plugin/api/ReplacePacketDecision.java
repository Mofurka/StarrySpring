package irden.space.proxy.plugin.api;


import irden.space.proxy.protocol.packet.PacketEnvelope;
import org.jetbrains.annotations.Nullable;

public record ReplacePacketDecision(
        PacketEnvelope envelope,
        @Nullable Runnable afterForward
) implements PacketDecision {

    public ReplacePacketDecision(PacketEnvelope envelope) {
        this(envelope, null);
    }
}
