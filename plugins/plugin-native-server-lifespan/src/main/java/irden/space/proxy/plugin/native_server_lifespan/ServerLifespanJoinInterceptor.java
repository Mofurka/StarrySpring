package irden.space.proxy.plugin.native_server_lifespan;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBooleanProperty(
        value = "native-server-lifespan.enabled",
        matchIfMissing = true
)
public class ServerLifespanJoinInterceptor {

    private static final String BOOTING_MESSAGE = "Server is still booting up. Please reconnect in a few seconds.";

    private final ServerLifespan serverLifespan;


    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext ctx) {
        if (!isBooting()) {
            return PacketDecision.forward();
        }
        return PacketDecision.cancel(
                () -> ctx.session().sendToClient(PacketType.PROTOCOL_RESPONSE, new ProtocolResponse(1, null))
        );
    }

    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext ctx) {
        if (!isBooting()) {
            return PacketDecision.forward();
        }
        return PacketDecision.cancel(
                () -> ctx.session().sendToClient(PacketType.CONNECT_FAILURE, new ConnectFailure(BOOTING_MESSAGE))
        );
    }

    private boolean isBooting() {
        return !serverLifespan.isServerReady();
    }
}
