package irden.space.proxy.plugin.player_manager.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "player_permission_overrides")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerPermissionOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "player_uuid", nullable = false, length = 32)
    private String playerUuid;

    @Column(name = "permission_name", nullable = false, length = 255)
    private String permissionName;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "changed_by", length = 255)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
