package irden.space.proxy.plugin.player_manager.ip;

import irden.space.proxy.plugin.player_manager.persistence.model.PlayerEntity;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerIpHistoryEntity;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerIpHistoryRepository;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerIpHistoryService {

    private final PlayerIpHistoryRepository ipHistoryRepository;
    private final PlayerRepository playerRepository;


    @Async
    public void recordConnection(String playerUuid, String ipAddress) {
        if (playerUuid == null || playerUuid.isBlank() || ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        try {
            ipHistoryRepository.recordConnection(playerUuid, ipAddress.trim(), LocalDateTime.now());
        } catch (RuntimeException e) {
            log.warn("Failed to record IP history: uuid={}, ip={}", playerUuid, ipAddress, e);
        }
    }


    public List<PlayerIpHistoryEntity> knownAddresses(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return List.of();
        }
        return ipHistoryRepository.findByPlayerUuidOrderByLastSeenDesc(playerUuid);
    }


    public List<PlayerLink> findLinks(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return List.of();
        }

        List<PlayerIpLink> rows = ipHistoryRepository.findLinkedPlayers(playerUuid);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<String, List<PlayerIpLink>> byPlayer = rows.stream()
                .collect(Collectors.groupingBy(PlayerIpLink::playerUuid, LinkedHashMap::new, Collectors.toList()));

        Map<String, String> namesByUuid = resolveNames(byPlayer.keySet());

        List<PlayerLink> links = new ArrayList<>(byPlayer.size());
        byPlayer.forEach((uuid, playerRows) -> {
            List<String> addresses = playerRows.stream()
                    .map(PlayerIpLink::ipAddress)
                    .distinct()
                    .toList();

            LocalDateTime lastSeen = playerRows.stream()
                    .map(PlayerIpLink::lastSeen)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);

            long connections = playerRows.stream().mapToLong(PlayerIpLink::connectionCount).sum();

            links.add(new PlayerLink(uuid, namesByUuid.get(uuid), addresses, lastSeen, connections));
        });

        links.sort(Comparator.comparing(
                PlayerLink::lastSeen,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));

        return links;
    }

    private Map<String, String> resolveNames(Set<String> playerUuids) {
        return playerRepository.findByPlayerUuidIn(playerUuids).stream()
                .collect(Collectors.toMap(
                        PlayerEntity::getPlayerUuid,
                        PlayerEntity::getName,
                        (left, right) -> left
                ));
    }
}
