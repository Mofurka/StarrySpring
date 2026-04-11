package irden.space.proxy.protocol.payload.packet.server_info;

public record ServerInfo(
        int players,
        int maxPlayers
) {
}
