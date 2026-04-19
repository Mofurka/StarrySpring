package irden.space.proxy.protocol.payload.packet.entity.destroy;

public record EntityDestroy(
        int entityId,
        byte[] finalNetState, // i dont want to parse it, and i dont need it tho
        boolean death
) {
}
