package irden.space.proxy.plugin.osb_detector;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.utils.messages.MessageUtils;
import irden.space.proxy.protocol.codec.variant.IntVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.client_connect.ClientConnect;
import irden.space.proxy.protocol.payload.packet.connect.ConnectFailure;
import irden.space.proxy.protocol.payload.packet.protocol_response.ProtocolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(
        value = "osb-detector.enabled"
)
public final class OsbDetectorJoinInterceptor {
    private final MessageUtils messageUtils;
    private final OsbDetectorConfiguration configuration;

    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext ctx) {
        Object o = ctx.parsedPayload();
        log.warn(o.toString());
        return PacketDecision.forward();
    }


    @PacketHandler(value = PacketType.PROTOCOL_RESPONSE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onProtocolResponse(PacketInterceptionContext ctx) {
        var pr = ctx.parsedPayload(ProtocolResponse.class);
        if (pr.info() == null) {
            return sendRejection(ctx);
        }
        return null;
    }

    @PacketHandler(value = PacketType.CLIENT_CONNECT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onClientConnect(PacketInterceptionContext ctx) {
        ClientConnect clientConnect = ctx.parsedPayload(ClientConnect.class);
        // Вообще сюда мы не можем зайти, так как отбрасываем ещё на ответе на протокол, но на всякий случай откину
        if (clientConnect.info() == null) {
            return sendRejection(ctx);
        }

        VariantValue info = clientConnect.info();
        Optional<VariantValue> brandOpt = Variants.get(info, "brand");
        Optional<VariantValue> openProtocolVersionOpt = Variants.get(info, "openProtocolVersion");

        if (brandOpt.isEmpty() || openProtocolVersionOpt.isEmpty()) {
            return sendRejection(ctx);
        }
        var brand = ((StringVariantValue) brandOpt.get()).value();
        var openProtocolVersion = ((IntVariantValue) openProtocolVersionOpt.get()).value();

        if (brand.equals("OpenStarbound") && openProtocolVersion >= configuration.osbProtocolVersionThreshold()) {
            return null;
        } else {
            String message = messageUtils.get("osb_detector.version.failure", configuration.osbVersion());
            return PacketDecision.cancel(() -> ctx.session().sendToClient(PacketType.CONNECT_FAILURE, new ConnectFailure(message)));
        }
    }

    private PacketDecision sendRejection(PacketInterceptionContext ctx) {
        String message = messageUtils.get("osb_detector.protocol.failure");
        return PacketDecision.forward(() -> ctx.session().sendToClient(PacketType.CONNECT_FAILURE, new ConnectFailure(message)));
    }


}
