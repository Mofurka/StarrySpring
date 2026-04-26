package irden.space.proxy.plugin.player_manager.persistence.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PlayerPermissionOverrideRecord(
        UUID id,
        String playerUuid,
        String permissionName,
        boolean granted,
        String changedBy,
        LocalDateTime changedAt
) {
}

