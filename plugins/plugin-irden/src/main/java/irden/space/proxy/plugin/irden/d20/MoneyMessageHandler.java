package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.discord.DiscordBotMessageService;
import irden.space.proxy.plugin.irden.d20.constants.MessageTemplate;
import irden.space.proxy.plugin.irden.d20.constants.RollMode;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class MoneyMessageHandler {

    private final PlayerManagerApi playerManagerApi;
    private final ISMMessenger messenger;
    private final DiscordBotMessageService discordBotMessageService;

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public void showMoney(Player sender, StatManagerMessage info) {
        long money = balance(sender);
        String message = money < 0
                ? MessageTemplate.DEBT_MESSAGE.replace("{money}", Long.toString(money))
                : MessageTemplate.MONEY_MESSAGE.replace("{money}", Long.toString(money));
        messenger.sendMessage(sender, List.of(sender.clientId()), message, RollMode.LOCAL, info);
    }

    public void transferMoney(Player sender, StatManagerMessage info) {
        String targetAlias = info.target() == null ? null : Variants.asString(info.target()).orElse(null);
        if (targetAlias == null) {
            return;
        }

        long count;
        try {
            count = Long.parseLong(safe(info.amount()).trim());
        } catch (NumberFormatException e) {
            reply(sender, MessageTemplate.INVALID_AMOUNT, info);
            return;
        }

        Player target = playerManagerApi.findPlayer(targetAlias, true).orElse(null);
        if (target == null) {
            return;
        }
        if (target.clientId() == sender.clientId()) {
            reply(sender, MessageTemplate.SELF_TRANSFER_ERROR, info);
            return;
        }
        if (count <= 0) {
            reply(sender, MessageTemplate.INVALID_AMOUNT, info);
            return;
        }
        if (count > balance(sender)) {
            reply(sender, MessageTemplate.NOT_ENOUGH_MONEY, info);
            return;
        }

        transfer(sender, target, count);

        reply(target, MessageTemplate.MONEY_RECEIVED
                .replace("{sender}", sender.nickname())
                .replace("{amount}", Long.toString(count)), info);
        reply(sender, MessageTemplate.MONEY_SENT
                .replace("{target}", target.nickname())
                .replace("{amount}", Long.toString(count)), info);
        discordBotMessageService.handleInGameMessage(MessageTemplate.DISCORD_TRANSFER
                .replace("{sender}", sender.nickname())
                .replace("{target}", target.nickname())
                .replace("{amount}", Long.toString(count)));
    }

    private void reply(Player player, String message, StatManagerMessage info) {
        messenger.sendMessage(player, List.of(player.clientId()), message, RollMode.LOCAL, info);
    }

    private long balance(Player player) {
        return 0L;
    }

    private void transfer(Player from, Player to, long amount) {
        // TODO: выполнить перевод через AccountService.
    }
}
