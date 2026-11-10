package irden.space.proxy.plugin.player_manager.ip;

import java.time.LocalDateTime;


public record PlayerIpLink(
        String playerUuid,
        String ipAddress,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        long connectionCount
) {
}
