package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "native-server-lifespan",
        name = "Native Server Lifespan",
        version = "1.0.0",
        dependsOn = {"command-handler", "general"},
        author = "https://github.com/Mofurka",
        description = "A plugin that is intended for actual starbound game server control."
)
@Component
@EnableConfigurationProperties({NativeServerLifespanConfig.class})
public final class NativeServerLifespanPlugin implements ProxyPlugin {

}
