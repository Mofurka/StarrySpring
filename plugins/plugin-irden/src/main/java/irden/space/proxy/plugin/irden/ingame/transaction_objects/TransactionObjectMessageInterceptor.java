package irden.space.proxy.plugin.irden.ingame.transaction_objects;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.discord.DiscordBotMessageService;
import irden.space.proxy.plugin.irden.account.StructureAccountType;
import irden.space.proxy.plugin.irden.ingame.transaction_objects.model.TransactionObjectData;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.VariantObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransactionObjectMessageInterceptor {
    private final AccountService accountService;
    private final AccountTransactionService accountTransactionService;
    private final VariantObjectMapper variantObjectMapper;
    private final PlayerManagerApi playerManagerApi;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final DiscordBotMessageService discordBotMessageService;

    @EntityMessageHandler("irden:account:name")
    public VariantValue getAccountName(EntityMessageContext context) {
        Optional<String> string = Variants.asString(context.arg(0));
        if (string.isPresent()) {
            AccountEntity account = accountService.getAccount(UUID.fromString(string.get()));
            String ownerName = account.getOwnerName();
            String code = StructureAccountType.valueOf(account.getAccountCode()).displayName();
            return Variants.listOf(code, ownerName);
        }
        return null;
    }

    @EntityMessageHandler("irden:account:transfer")
    public VariantValue transferMoney(EntityMessageContext context) {
        TransactionObjectData transactionObjectData = variantObjectMapper.fromVariant(context.arg(0), TransactionObjectData.class);
        Long amount = transactionObjectData.amount();
        String account = transactionObjectData.account();
        if (amount <= 0) {
            return Variants.of("Сумма не может быть меньше нуля");
        }

        Optional<Player> playerBySessionId = playerManagerApi.findPlayerBySessionId(context.session().sessionId());
        if (playerBySessionId.isPresent()) {
            UUID playerAccountUuid = (UUID) playerBySessionId.get().metadata().get("accountUuid");
            try {
                UUID uuid = UUID.fromString(account);
                AccountTransactionEntity byObject = accountTransactionService.transfer(playerAccountUuid, uuid, amount, UUID.randomUUID(), "By object");
                AccountEntity account1 = accountService.getAccount(uuid);
                playerBySessionId.get().sendMessage("Вы перевели %s %s -> %s %s".formatted(byObject.getAmount(), coins(byObject.getAmount()), StructureAccountType.valueOf(account1.getAccountCode()).displayName(), account1.getOwnerName()));
                discordBotMessageService.handleInGameMessage("**%s** перевёл %s %s -> %s %s".formatted(playerBySessionId.get().name(), byObject.getAmount(), coins(byObject.getAmount()), StructureAccountType.valueOf(account1.getAccountCode()).displayName(), account1.getOwnerName()));
            } catch (InsufficientFundsException e) {
                return Variants.of("Недостаточно средств");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
    private static String coins(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монету", "монеты", "монет");
    }
}
