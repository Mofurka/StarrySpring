package irden.space.proxy.plugin.site.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;


@Entity
@Table(name = "player_attributes")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAttributesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "player_uuid", nullable = false, length = 32)
    private String playerUuid;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "discord_id", nullable = false)
    private Long discordId;
}
