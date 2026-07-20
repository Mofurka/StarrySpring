package irden.space.proxy.plugin.ban_manager.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ban_records", schema = "ban_manager")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BanRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "player_uuid", length = 32)
    private String playerUuid;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "banned_by", nullable = false, length = 255)
    private String bannedBy;

    @Column(name = "permanent", nullable = false)
    private boolean permanent;

    @Column(name = "banned_at", nullable = false)
    private LocalDateTime bannedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
