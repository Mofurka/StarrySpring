package irden.space.proxy.plugin.player_manager.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "players", schema = "player_manager")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "player_uuid", nullable = false, unique = true, length = 32)
    private String playerUuid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen", nullable = false)
    @Builder.Default
    private  LocalDateTime lastSeen = LocalDateTime.now();
}
