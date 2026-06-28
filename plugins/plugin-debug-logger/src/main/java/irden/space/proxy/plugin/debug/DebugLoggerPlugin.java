package irden.space.proxy.plugin.debug;

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
        id = "debug-logger",
        name = "Debug Logger",
        version = "1.0.0",
        dependsOn = {"command-handler"},
        author = "https://github.com/Mofurka",
        description = "A plugin that logs all packets and lifecycle events for debugging purposes. And also my test area."
)
@Component
public final class DebugLoggerPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(DebugLoggerPlugin.class);


    @OnLoad
    public void handleLoad() {
        log.info("Loading plugin. Loading plugin after rebuild runtime'{}'", descriptor().id());
    }


    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }

    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext context) {
        return logPacket("onProtocolRequest", context);
    }

    @PacketHandler(value = PacketType.PROTOCOL_RESPONSE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onProtocolResponse(PacketInterceptionContext context) {
        return logPacket("onProtocolResponse", context);
    }

    @PacketHandler(value = PacketType.MODIFY_TILE_LIST)
    public PacketDecision onModifyTileList(PacketInterceptionContext context) {
        return logPacket("onModifyTileList", context);
    }

    private PacketDecision logPacket(String handlerName, PacketInterceptionContext context) {
        log.info(
                "[PLUGIN][{}][{}] packetType={} parsed={}",
                handlerName,
                context.direction(),
                context.envelope().packetType(),
                context.parsedPayload()
        );
        return PacketDecision.forward();
    }

}
