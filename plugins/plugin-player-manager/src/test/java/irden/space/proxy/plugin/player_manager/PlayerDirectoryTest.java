package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.api.DefaultPlayerManagerApi;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDirectoryTest {

    @Test
    void exposesOfflinePlayersThroughSpringApiImplementation() {
        PlayerRecord alice = player("00000000000000000000000000000001", "Alice", "127.0.0.1");
        PlayerRecord alicia = player("00000000000000000000000000000002", "Alicia", "127.0.0.1");
        PlayerDirectory directory = new PlayerDirectory(new InMemoryPlayerRegistry(), new StubPlayerRepository(List.of(alice, alicia)));
        PlayerManagerApi api = new DefaultPlayerManagerApi(directory);

        Optional<Player> found = api.findPlayer("Alice", false);
        List<Player> matches = api.searchPlayers("ali", 10, false);

        assertTrue(found.isPresent());
        assertEquals("Alice", found.orElseThrow().name());
        assertEquals(List.of("Alice", "Alicia"), matches.stream().map(Player::name).toList());
        assertEquals(2, api.findAllPlayersByIpAddress("127.0.0.1").size());
    }

    private PlayerRecord player(String uuid, String name, String ipAddress) {
        return PlayerRecord.builder()
                .playerUuid(uuid)
                .name(name)
                .ipAddress(ipAddress)
                .build();
    }

    private static final class StubPlayerRepository extends PlayerJdbcRepository {
        private final List<PlayerRecord> players;

        private StubPlayerRepository(List<PlayerRecord> players) {
            super(new JdbcTemplate());
            this.players = players;
        }

        @Override
        public Optional<PlayerRecord> findByName(String name) {
            return players.stream().filter(player -> player.name().equals(name)).findFirst();
        }

        @Override
        public Optional<PlayerRecord> findByUuid(String uuid) {
            return players.stream().filter(player -> player.playerUuid().equals(uuid)).findFirst();
        }

        @Override
        public List<PlayerRecord> findByIpAddress(String ipAddress) {
            return players.stream().filter(player -> player.ipAddress().equals(ipAddress)).toList();
        }

        @Override
        public List<PlayerRecord> findAll() {
            return players;
        }
    }
}
