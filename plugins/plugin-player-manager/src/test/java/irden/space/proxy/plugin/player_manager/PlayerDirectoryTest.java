package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.api.DefaultPlayerManagerApi;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.player_manager.persistence.model.PlayerEntity;
import irden.space.proxy.plugin.player_manager.persistence.repository.PlayerRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDirectoryTest {

    /**
     * {@link PlayerRepository} is a Spring Data interface, so the test backs it with a dynamic proxy
     * that only answers the finder methods {@link PlayerDirectory} actually calls.
     */
    private static PlayerRepository stubRepository(List<PlayerEntity> players) {
        return (PlayerRepository) Proxy.newProxyInstance(
                PlayerDirectoryTest.class.getClassLoader(),
                new Class<?>[]{PlayerRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByName" -> players.stream()
                            .filter(player -> player.getName().equals(args[0]))
                            .findFirst();
                    case "findByPlayerUuid" -> players.stream()
                            .filter(player -> player.getPlayerUuid().equals(args[0]))
                            .findFirst();
                    case "findByIpAddress" -> players.stream()
                            .filter(player -> player.getIpAddress().equals(args[0]))
                            .toList();
                    case "findAll" -> players;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    @Test
    void exposesOfflinePlayersThroughSpringApiImplementation() {
        PlayerEntity alice = player("00000000000000000000000000000001", "Alice", "127.0.0.1");
        PlayerEntity alicia = player("00000000000000000000000000000002", "Alicia", "127.0.0.1");
        PlayerDirectory directory = new PlayerDirectory(new InMemoryPlayerRegistry(), stubRepository(List.of(alice, alicia)));
        PlayerManagerApi api = new DefaultPlayerManagerApi(directory);

        Optional<Player> found = api.findPlayer("Alice", false);
        List<Player> matches = api.searchPlayers("ali", 10, false);

        assertTrue(found.isPresent());
        assertEquals("Alice", found.orElseThrow().name());
        assertEquals(List.of("Alice", "Alicia"), matches.stream().map(Player::name).toList());
        assertEquals(2, api.findAllPlayersByIpAddress("127.0.0.1").size());
    }

    private PlayerEntity player(String uuid, String name, String ipAddress) {
        return PlayerEntity.builder()
                .playerUuid(uuid)
                .name(name)
                .ipAddress(ipAddress)
                .build();
    }
}
