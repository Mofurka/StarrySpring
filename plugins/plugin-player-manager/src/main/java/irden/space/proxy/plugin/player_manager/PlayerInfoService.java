package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.api.PlayerInfoField;
import irden.space.proxy.plugin.player_manager.api.PlayerInfoRegistry;
import irden.space.proxy.plugin.player_manager.api.PlayerInfoSection;
import irden.space.proxy.plugin.player_manager.ip.PlayerIpHistoryService;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerEntity;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerIpHistoryEntity;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRoleEntity;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerRepository;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class PlayerInfoService {

    private static final DateTimeFormatter SEEN_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String NONE = "none";

    private final PlayerRepository playerRepository;
    private final PlayerRoleRepository playerRoleRepository;
    private final PlayerIpHistoryService ipHistoryService;
    private final PlayerInfoRegistry playerInfoRegistry;


    public String describe(Player player) {
        String playerUuid = player.uuid() == null ? null : player.uuid().toString();

        StringBuilder sb = new StringBuilder();
        sb.append("Player info:").append(System.lineSeparator());
        appendFields(sb, baseFields(player, playerUuid), "- ");

        for (PlayerInfoSection section : playerInfoRegistry.collect(player)) {
            sb.append(section.title()).append(':').append(System.lineSeparator());
            appendFields(sb, section.fields(), "  - ");
        }

        return sb.toString();
    }

    private List<PlayerInfoField> baseFields(Player player, String playerUuid) {
        List<PlayerInfoField> fields = new ArrayList<>();
        fields.add(PlayerInfoField.of("Name", player.name()));
        fields.add(PlayerInfoField.of("UUID", playerUuid));
        fields.add(PlayerInfoField.of("Online", player.online()));

        if (player.online()) {
            fields.add(PlayerInfoField.of("Account", player.account()));
            fields.add(PlayerInfoField.of("Client ID", player.clientId()));
            fields.add(PlayerInfoField.of("Entity ID", player.entityId()));
            fields.add(PlayerInfoField.of("IP address", player.ipAddress()));
            appendLocation(fields, player);
        }

        appendStoredFacts(fields, player, playerUuid);
        appendAddresses(fields, playerUuid);
        appendRoles(fields, playerUuid);

        if (player.online()) {
            Map<String, Object> metadata = player.metadata();
            if (!metadata.isEmpty()) {
                metadata.forEach((key, value) -> fields.add(PlayerInfoField.of("meta." + key, value)));
            }
        }

        return fields;
    }

    private void appendLocation(List<PlayerInfoField> fields, Player player) {
        if (player.position() == null) {
            return;
        }
        if (player.position().getPreviousLocation() != null) {
            fields.add(PlayerInfoField.of("Previous location", player.position().getPreviousLocation()));
        }
        if (player.position().getCurrentLocation() != null) {
            fields.add(PlayerInfoField.of("Current location", player.position().getCurrentLocation()));
        }
        if (player.position().getX() != null || player.position().getY() != null) {
            fields.add(PlayerInfoField.of(
                    "Position",
                    "{" + player.position().getX() + "," + player.position().getY() + "}"
            ));
        }
    }


    private void appendStoredFacts(List<PlayerInfoField> fields, Player player, String playerUuid) {
        Optional<PlayerEntity> stored = playerUuid == null
                ? Optional.empty()
                : playerRepository.findByPlayerUuid(playerUuid);

        if (stored.isEmpty()) {
            fields.add(PlayerInfoField.of("First seen", "not recorded"));
            if (!player.online() && player.lastSeen() != null) {
                fields.add(PlayerInfoField.of("Last seen", SEEN_AT.format(player.lastSeen())));
            }
            return;
        }

        PlayerEntity entity = stored.get();
        fields.add(PlayerInfoField.of("First seen", format(entity.getCreatedAt())));
        fields.add(PlayerInfoField.of("Last seen", format(entity.getLastSeen())));
        if (!player.online()) {
            fields.add(PlayerInfoField.of("Last known IP", entity.getIpAddress()));
        }
    }

    private void appendAddresses(List<PlayerInfoField> fields, String playerUuid) {
        if (playerUuid == null) {
            return;
        }

        List<PlayerIpHistoryEntity> addresses = ipHistoryService.knownAddresses(playerUuid);
        if (addresses.isEmpty()) {
            return;
        }

        long connections = addresses.stream().mapToLong(PlayerIpHistoryEntity::getConnectionCount).sum();
        fields.add(PlayerInfoField.of("Known IPs", addresses.size() + " (" + connections + " connections)"));

        int linkCount = ipHistoryService.findLinks(playerUuid).size();
        fields.add(PlayerInfoField.of("Linked players", linkCount == 0 ? NONE : linkCount + " (/user links)"));
    }

    private void appendRoles(List<PlayerInfoField> fields, String playerUuid) {
        if (playerUuid == null) {
            return;
        }

        List<String> roles = playerRoleRepository.findByPlayerUuidOrderByAssignedAtAscRoleNameAsc(playerUuid)
                .stream()
                .map(PlayerRoleEntity::getRoleName)
                .toList();

        fields.add(PlayerInfoField.of("Roles", roles.isEmpty() ? NONE : String.join(", ", roles)));
    }

    private void appendFields(StringBuilder sb, List<PlayerInfoField> fields, String prefix) {
        for (PlayerInfoField field : fields) {
            sb.append(prefix).append(field.name()).append(": ").append(field.value())
                    .append(System.lineSeparator());
        }
    }

    private String format(LocalDateTime value) {
        return value == null ? "-" : SEEN_AT.format(value);
    }
}
