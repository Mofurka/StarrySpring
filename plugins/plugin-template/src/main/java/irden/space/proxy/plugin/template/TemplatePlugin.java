package irden.space.proxy.plugin.site;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@PluginDefinition(
        id = "template",
        name = "Template Plugin",
        version = "1.0.0",
        dependsOn = {""},
        author = "https://github.com/Mofurka",
        description = ""
)
@Component
public final class TemplatePlugin implements ProxyPlugin {


}
