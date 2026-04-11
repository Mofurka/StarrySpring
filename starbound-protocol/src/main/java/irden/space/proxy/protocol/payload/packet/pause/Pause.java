package irden.space.proxy.protocol.payload.packet.pause;

public record Pause(
        boolean pause,
        float timescale
) {
}
