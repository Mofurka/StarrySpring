package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerEntity;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerRepository;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public final class PlayerDirectory {

    private final PlayerRegistry<Player> players;
    private final PlayerRepository playerRepository;

    public PlayerDirectory(
            @Qualifier("onlinePlayerRegistry") PlayerRegistry<Player> players,
            PlayerRepository playerRepository
    ) {
        this.players = players;
        this.playerRepository = playerRepository;
    }

    public List<Player> onlinePlayers() {
        return players.getAll();
    }
    public Optional<Player> findPlayer(String identifier, boolean loggedIn) {
        boolean isStarUuid = identifier.matches("^[0-9a-fA-F]{32}$");
        boolean isClientId = identifier.matches("^\\d+$");
        Optional<Player> onlinePlayer = players.getAll().stream()
                .filter(player -> player.name().equals(identifier)
                        || (isStarUuid && player.uuid().toString().equals(identifier))
                        || (isClientId && Integer.toString(player.clientId()).equals(identifier)))
                .findFirst();
        if (onlinePlayer.isPresent() || loggedIn) {
            return onlinePlayer;
        }

        Optional<PlayerEntity> playerRecord = playerRepository.findFirstByName(identifier);
        if (playerRecord.isEmpty()) {
            playerRecord = playerRepository.findByPlayerUuid(identifier);
        }
        return playerRecord.map(this::toOfflinePlayer);
    }

    public List<Player> searchPlayers(String prefix, int limit, boolean loggedIn) {
        String normalizedPrefix = prefix == null ? "" : prefix.trim().toLowerCase(Locale.ROOT);
        int safeLimit = limit <= 0 ? Integer.MAX_VALUE : limit;

        Stream<Player> source = loggedIn
                ? players.getAll().stream()
                : Stream.concat(
                        players.getAll().stream(),
                        playerRepository.findAll().stream().map(this::toOfflinePlayer)
                );

        Map<String, Player> uniquePlayers = source
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        player -> player.uuid().toString(),
                        Function.identity(),
                        this::preferOnlinePlayer,
                        LinkedHashMap::new
                ));

        return uniquePlayers.values().stream()
                .filter(player -> matchesPlayerSearch(player, normalizedPrefix))
                .sorted(Comparator
                        .comparingInt((Player player) -> playerSearchRank(player, normalizedPrefix))
                        .thenComparing(Player::online, Comparator.reverseOrder())
                        .thenComparing(Player::name, String.CASE_INSENSITIVE_ORDER))
                .limit(safeLimit)
                .toList();
    }

    public List<Player> findAllPlayersByIpAddress(String ipAddress) {
        List<Player> onlinePlayers = players.getAll().stream()
                .filter(player -> player.ipAddress().equals(ipAddress))
                .toList();
        List<Player> offlinePlayers = playerRepository.findByIpAddress(ipAddress).stream()
                .map(this::toOfflinePlayer)
                .toList();
        return Stream.concat(onlinePlayers.stream(), offlinePlayers.stream())
                .distinct()
                .toList();
    }

    public Optional<Player> getPlayerByClientId(int clientId) {
        return players.getAll().stream()
                .filter(player -> player.clientId() == clientId)
                .findFirst();
    }

    public Optional<Player> getPlayerBySessionId(String sessionId) {
        return Optional.ofNullable(players.getBySessionId(sessionId));
    }

    public Optional<Player> getPlayerByName(String name, boolean loggedIn) {
        if (loggedIn) {
            return players.getAll().stream()
                    .filter(player -> player.name().equals(name))
                    .findFirst();
        }
        return playerRepository.findFirstByName(name).map(this::toOfflinePlayer);
    }

    public Optional<Player> getPlayerByUuid(String uuid, boolean loggedIn) {
        if (loggedIn) {
            return players.getAll().stream()
                    .filter(player -> player.uuid().toString().equals(uuid))
                    .findFirst();
        }
        return playerRepository.findByPlayerUuid(uuid).map(this::toOfflinePlayer);
    }

    private Player toOfflinePlayer(PlayerEntity record) {
        return Player.builder()
                .name(record.getName())
                .uuid(StarUuid.fromHex(record.getPlayerUuid()))
                .ipAddress(record.getIpAddress())
                .build();
    }

    private Player preferOnlinePlayer(Player left, Player right) {
        if (left.online() == right.online()) {
            return left;
        }
        return left.online() ? left : right;
    }

    private boolean matchesPlayerSearch(Player player, String normalizedPrefix) {
        if (normalizedPrefix.isBlank()) {
            return true;
        }

        String playerName = player.name().toLowerCase(Locale.ROOT);
        String playerUuid = player.uuid().toString().toLowerCase(Locale.ROOT);
        String clientId = player.online() ? Integer.toString(player.clientId()) : "";
        return playerName.contains(normalizedPrefix)
                || playerUuid.startsWith(normalizedPrefix)
                || clientId.startsWith(normalizedPrefix);
    }

    private int playerSearchRank(Player player, String normalizedPrefix) {
        if (normalizedPrefix.isBlank()) {
            return player.online() ? 0 : 1;
        }

        String playerName = player.name().toLowerCase(Locale.ROOT);
        String playerUuid = player.uuid().toString().toLowerCase(Locale.ROOT);
        String clientId = player.online() ? Integer.toString(player.clientId()) : "";
        if (playerName.equals(normalizedPrefix)) {
            return 0;
        }
        if (playerName.startsWith(normalizedPrefix)) {
            return 1;
        }
        if (playerName.contains(normalizedPrefix)) {
            return 2;
        }
        if (playerUuid.startsWith(normalizedPrefix)) {
            return 3;
        }
        if (clientId.startsWith(normalizedPrefix)) {
            return 4;
        }
        return 5;
    }

}
