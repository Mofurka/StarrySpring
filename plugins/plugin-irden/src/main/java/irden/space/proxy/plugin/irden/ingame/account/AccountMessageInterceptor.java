package irden.space.proxy.plugin.irden.ingame.account;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountEntity;
import irden.space.proxy.plugin.irden.service.PlayerAccountService;
import irden.space.proxy.plugin.irden.service.exception.AccountNotFoundException;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AccountMessageInterceptor {
    private final PlayerAccountService playerAccountService;
    private final PlayerManagerApi playerManagerApi;

    @EntityMessageHandler("irden:myMoney")
    public VariantValue handlePlayerMoneyRequest(EntityMessageContext ctx) {
        Optional<Player> playerBySessionId = playerManagerApi.findPlayerBySessionId(ctx.session().sessionId());
        if (playerBySessionId.isPresent()) {
            AccountEntity mainAccount = playerAccountService.getMainAccount(playerBySessionId.get().uuid());
            long balance = mainAccount.getBalance();
            return Variants.of(balance);
        }
        throw new AccountNotFoundException("Аккаунт не найден");
    }


}
