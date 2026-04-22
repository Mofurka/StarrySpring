package irden.space.proxy.plugin.player_manager.persistence.model;

import java.time.LocalDateTime;

public record BanRecord(
        String name,
        String playerUuid,
        String ipAddress,
        String reason,
        String bannedBy,
        boolean permanent,
        LocalDateTime bannedAt,
        LocalDateTime expiresAt
) {
}
