package irden.space.proxy.plugin.player_manager.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(
        name = "player_ip_history",
        schema = "player_manager",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_player_manager_ip_history_uuid_ip",
                columnNames = {"player_uuid", "ip_address"}
        ),
        indexes = @Index(name = "idx_player_manager_ip_history_ip", columnList = "ip_address")
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerIpHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "player_uuid", nullable = false, length = 32)
    private String playerUuid;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "connection_count", nullable = false)
    @Builder.Default
    private long connectionCount = 1;
}
