package irden.space.proxy.plugin.player_manager.persistence.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
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
