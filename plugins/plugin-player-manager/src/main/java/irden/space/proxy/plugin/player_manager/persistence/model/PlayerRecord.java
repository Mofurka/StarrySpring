package irden.space.proxy.plugin.player_manager.persistence.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PlayerRecord(
        UUID id,
        String playerUuid,
        String name,
        String ipAddress,
        LocalDateTime createdAt
) {
}