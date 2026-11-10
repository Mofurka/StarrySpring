package irden.space.proxy.plugin.player_manager.ip;

import java.time.LocalDateTime;
import java.util.List;


public record PlayerLink(
        String playerUuid,
        String name,
        List<String> sharedAddresses,
        LocalDateTime lastSeen,
        long connectionCount
) {
}
