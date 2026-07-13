package irden.space.proxy.plugin.api;

import org.jetbrains.annotations.Nullable;

public record ForwardPacketDecision(
        @Nullable Runnable afterForward
) implements PacketDecision {

    public static final ForwardPacketDecision INSTANCE =
            new ForwardPacketDecision(null);
}