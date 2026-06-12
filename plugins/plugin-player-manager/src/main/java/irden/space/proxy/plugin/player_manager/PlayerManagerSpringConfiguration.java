package irden.space.proxy.plugin.player_manager;

import irden.space.proxy.plugin.player_manager.api.DefaultPlayerManagerApi;
import irden.space.proxy.plugin.player_manager.permissions.PermissionResolver;
import irden.space.proxy.plugin.player_manager.persistence.PlayerAccessJdbcRepository;
import irden.space.proxy.plugin.player_manager.persistence.PlayerJdbcRepository;
import irden.space.proxy.plugin.player_manager.roles.RoleManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({
        InMemoryConnectingPlayers.class,
        InMemoryPlayerRegistry.class,
        PlayerDirectory.class,
        DefaultPlayerManagerApi.class,
        PermissionResolver.class,
        PlayerAccessJdbcRepository.class,
        PlayerJdbcRepository.class,
        RoleManager.class
})
public class PlayerManagerSpringConfiguration {
}
