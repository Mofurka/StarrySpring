package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnStart;
import irden.space.proxy.plugin.utils.messages.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "irden",
        name = "Irden Plugin",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        dependsOn = {"command-handler", "player-manager", "discord-bot", "general", "star-custom-chat", "native-server-lifespan"},
        description = "Irden gavno ebanoe"
)
@Component
@Slf4j
public final class IrdenPlugin implements ProxyPlugin {
    private final MessageUtils messageUtils;


    public IrdenPlugin(MessageUtils messageUtils) {
        this.messageUtils = messageUtils;
    }

    @OnStart
    public void test() {
        String s = messageUtils.get("test");
        log.info("Test message: {}", s);
    }


}
