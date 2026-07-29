package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.star_custom_chat.constants.ChatMode;
import irden.space.proxy.plugin.star_custom_chat.constants.SCCConstants;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StarCustomChatMessageSender {
    private final EntityMessageService entityMessageService;
    private final PlayerManagerApi playerManagerApi;

    public void sendMessageToSCC(Player target, String nickname, String message, ChatMode chatMode) {
        entityMessageService.sendToEntity(target.sessionContext(),
                target.entityId(),
                SCCConstants.SCC_ADD_MESSAGE,
                Variants.mapOf(
                        Map.of(
                                "connection", 0,
                                "mode", chatMode.getMode(),
                                "nickname", nickname,
                                "text", chatMode.getPrefix() + " " + message,
                                "proximityRadius", 100
                        )
                )
        );
    }

    public void broadcastMessage(String nickname, String message, ChatMode chatMode) {
        playerManagerApi.onlinePlayers().forEach(player -> this.sendMessageToSCC(player, nickname, message, chatMode));
    }

    public void broadcastMessageToUuids(Collection<String> uuids, String message, ChatMode chatMode) {
        if (uuids.isEmpty()) return;
        uuids.forEach(uuid -> playerManagerApi.findPlayerByUuid(uuid, true)
                .ifPresent(player -> this.sendMessageToSCC(player, "Server", message, chatMode)));
    }

    public void broadcastMessageToClientIds(Collection<Integer> clientIds, String message, ChatMode chatMode) {
        if (clientIds.isEmpty()) return;
        clientIds.forEach(
                clientId -> playerManagerApi.findByClientId(clientId).ifPresent(
                        player -> this.sendMessageToSCC(player, "Server", message, chatMode)));
    }


}
