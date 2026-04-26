package irden.space.proxy.plugin.player_manager.persistence.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record PlayerRoleRecord(
        UUID id,
        String playerUuid,
        String roleName,
        String assignedBy,
        LocalDateTime assignedAt
) {
}

