package irden.space.proxy.plugin.api;

import org.jetbrains.annotations.Nullable;

public record DropPacketDecision(
        @Nullable Runnable afterDrop
) implements PacketDecision {

    public static final DropPacketDecision INSTANCE =
            new DropPacketDecision(null);
}