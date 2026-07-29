package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.general.events.ChatMessageEvent;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.star_custom_chat.constants.SCCConstants;
import irden.space.proxy.plugin.star_custom_chat.model.IrdenCustomChatFightData;
import irden.space.proxy.plugin.star_custom_chat.model.IrdenCustomChatProximityData;
import irden.space.proxy.plugin.star_custom_chat.model.IrdenCustomChatSH;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StarCustomChatMessageInterceptor {
    private final JsonMapper jsonMapper;
    private final PlayerManagerApi playerManagerApi;
    private final ApplicationEventPublisher eventPublisher;


    @PacketHandler(
            value = PacketType.SPAWN_ENTITY,
            direction = PacketDirection.TO_SERVER
    )
    private PacketDecision onSpawnEntity(PacketInterceptionContext ctx) {
        Entity entity = ctx.parsedPayload(Entity.class);

        if (!(entity instanceof StageHandEntity(VariantValue payload))) {
            return PacketDecision.forward();
        }

        JsonNode jsonNode = MapVariantUtils.variantToJsonNode(payload);
        JsonNode typeNode = jsonNode.get("type");

        if (typeNode == null
                || !SCCConstants.IRDEN_CUSTOM_CHAT.equals(typeNode.asString())) {
            return PacketDecision.forward();
        }

        var messageNode = jsonNode.get("message");

        if (messageNode == null
                || (!SCCConstants.SEND_PROXY_MESSAGE.equals(messageNode.asString()))
                && !SCCConstants.SCC_ADD_MESSAGE.equals(messageNode.asString())) {
            return PacketDecision.forward();
        }

        try {
            IrdenCustomChatSH customChat =
                    jsonMapper.treeToValue(jsonNode, IrdenCustomChatSH.class);
            Optional<Player> playerBySessionId = playerManagerApi.getPlayerBySessionId(ctx.session().sessionId());
            if (playerBySessionId.isPresent()) {
                var player = playerBySessionId.get();
                ChatMessageEvent chatMessageEvent = null;
                if (customChat.data() instanceof IrdenCustomChatProximityData proximityData) {
                    chatMessageEvent = new ChatMessageEvent(player, proximityData.getMode().toString(), proximityData.getText(), Map.of("proximityRadius", proximityData.getProximityRadius()));
                } else if (customChat.data() instanceof IrdenCustomChatFightData fightData) {
                    chatMessageEvent = new ChatMessageEvent(player, fightData.getMode().toString(), fightData.getText(), Map.of("fightName", fightData.getFight()));
                }
                if (chatMessageEvent != null) {
                    eventPublisher.publishEvent(chatMessageEvent);
                }
            }


        } catch (Exception e) {
            log.warn(
                    "Не удалось преобразовать StageHand payload: {}. Причина: {}",
                    jsonNode,
                    e.getMessage(),
                    e
            );
        }

        return PacketDecision.forward();
    }

}
