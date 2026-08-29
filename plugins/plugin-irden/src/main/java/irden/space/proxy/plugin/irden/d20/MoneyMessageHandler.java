package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.discord.DiscordBotMessageService;
import irden.space.proxy.plugin.irden.d20.constants.MessageTemplate;
import irden.space.proxy.plugin.irden.d20.constants.RollMode;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.plugin.star_custom_chat.StarCustomChatMessageSender;
import irden.space.proxy.plugin.star_custom_chat.constants.ChatMode;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class MoneyMessageHandler {

    private final PlayerManagerApi playerManagerApi;
    private final ISMMessenger messenger;
    private final DiscordBotMessageService discordBotMessageService;
    private final AccountTransactionService accountTransactionService;
    private final StarCustomChatMessageSender starCustomChatMessageSender;
    private final AccountService accountService;

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public void showMoney(Player sender, StatManagerMessage info) {
        long money = balance(sender);
        String message = money < 0
                ? MessageTemplate.DEBT_MESSAGE.replace("{money}", Long.toString(money))
                : MessageTemplate.MONEY_MESSAGE.replace("{money}", Long.toString(money));
        reply(sender, message, info);
    }

    public void transferMoney(Player sender, StatManagerMessage info) {
        String targetAlias = info.target() == null ? null : Variants.asString(info.target()).orElse(null);
        if (targetAlias == null) {
            return;
        }

        long count;
        try {
            count = info.amount();
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
//        messenger.sendMessage(player, List.of(player.clientId()), message, RollMode.LOCAL, info);
        starCustomChatMessageSender.sendMessageToSCC(player, "Server", message, ChatMode.PROXIMITY);
    }

    private long balance(Player player) {
        UUID accountUuid = (UUID) player.metadata().get("accountUuid");
        AccountEntity account = accountService.getAccount(accountUuid);
        return account.getBalance();
    }

    private void transfer(Player from, Player to, long amount) {
        UUID fromAccountUuid = (UUID) from.metadata().get("accountUuid");
        UUID toAccountUuid = (UUID) to.metadata().get("accountUuid");
        try {
            accountTransactionService.transfer(fromAccountUuid, toAccountUuid, amount, UUID.randomUUID(), "Irden Stat Manager Operation");
        } catch (InsufficientFundsException e) {
            from.sendMessage("У вас недостаточно монет.");
        } catch (Exception e) {
            from.sendMessage(e.getMessage());
        }
    }

    private String getDeclinedAmount(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }
}
