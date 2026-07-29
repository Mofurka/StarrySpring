package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.command_handler.color.Color;
import irden.space.proxy.plugin.discord.DiscordBotMessageService;
import irden.space.proxy.plugin.general.GeneralUtils;
import irden.space.proxy.plugin.irden.d20.constants.Placeholders;
import irden.space.proxy.plugin.irden.d20.constants.RollMode;
import irden.space.proxy.plugin.irden.d20.initiative.IrdenFightHandler;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.star_custom_chat.StarCustomChatMessageSender;
import irden.space.proxy.plugin.star_custom_chat.constants.ChatMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ISMMessenger {

    private final DiscordBotMessageService discordBotMessageService;

    private final StarCustomChatMessageSender starCustomChatMessageSender;

    private final IrdenFightHandler fightHandler;

    private final GeneralUtils generalUtils;


    /**
     * @param rollMode {@link RollMode}
     */
    public void sendMessage(Player sender, List<Integer> clientIds, String message, String rollMode, StatManagerMessage info) {
        var discordMessage = "**<%s> [%s]:**".formatted(sender.nickname(), rollMode.toUpperCase()) + "`" + Color.stripColorCodes(message.replace(Placeholders.PLAYER, "").trim()) + "`";
        discordBotMessageService.handleInGameMessage(discordMessage);
        var sccMessage = message.trim().replace(Placeholders.PLAYER, Color.MAGENTA.colorString(sender.nickname()));
        switch (rollMode) {
            case RollMode.FIGHT -> {
                // a lil of O(n), but its nothing in case that there might be only one battle at time, or maybe two, but no more than 2-3. Also, the rolls are quite rare, maybe one in 1 minute
                var snapshot = fightHandler.findUuidFightSnapshot(sender.uuid().toString());
                if (snapshot == null) {
                    return;
                }
                List<String> queue = snapshot.queue();
                starCustomChatMessageSender.broadcastMessageToUuids(queue, sccMessage, ChatMode.FIGHT);
            }
            case RollMode.LOCAL ->
                    starCustomChatMessageSender.broadcastMessageToClientIds(clientIds, sccMessage, ChatMode.PROXIMITY);
            case RollMode.BROADCAST -> generalUtils.broadcastMessage(sccMessage);
            default -> starCustomChatMessageSender.sendMessageToSCC(sender, "Server", sccMessage, ChatMode.WHISPER);
        }
    }
}
