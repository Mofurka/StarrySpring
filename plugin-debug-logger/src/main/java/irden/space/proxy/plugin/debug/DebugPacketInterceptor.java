package irden.space.proxy.plugin.debug;


import irden.space.proxy.plugin_api.ForwardPacketDecision;
import irden.space.proxy.plugin_api.PacketDecision;
import irden.space.proxy.plugin_api.PacketInterceptionContext;
import irden.space.proxy.plugin_api.PacketInterceptor;
import irden.space.proxy.protocol.packet.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugPacketInterceptor implements PacketInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DebugPacketInterceptor.class);

    @Override
    public boolean supports(PacketInterceptionContext context) {
        return context.envelope().packetType() == PacketType.PROTOCOL_REQUEST
                || context.envelope().packetType() == PacketType.CHAT_RECEIVED;
    }

    @Override
    public PacketDecision intercept(PacketInterceptionContext context) {
        log.info(
                "[PLUGIN][{}] packetType={} parsed={}",
                context.direction(),
                context.envelope().packetType(),
                context.parsedPayload()
        );
        return ForwardPacketDecision.INSTANCE;
    }
}
