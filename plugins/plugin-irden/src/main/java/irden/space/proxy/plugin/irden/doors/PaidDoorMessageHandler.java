package irden.space.proxy.plugin.irden.doors;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.doors.model.IrdenDoorPaidEntryOffer;
import irden.space.proxy.plugin.irden.persistence.model.account.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.irden.service.exception.InsufficientFundsException;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.util.VariantObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaidDoorMessageHandler {
    private final VariantObjectMapper variantObjectMapper;
    private final AccountTransactionService transactionService;
    private final PlayerManagerApi playerManagerApi;
    private final EntityMessageService entityMessageService;


    @EntityMessageHandler("irdenDoor:paidEntryOffer")
    public void onPaidDoorRequest(EntityMessageContext ctx) {
        playerManagerApi.findPlayerBySessionId(ctx.session().sessionId()).ifPresent(player -> {
            var door = variantObjectMapper.fromVariant(ctx.arg(0), IrdenDoorPaidEntryOffer.class);
            UUID accountUuid = (UUID) player.metadata().get("accountUuid");
            try {
                var withdraw = transactionService.withdraw(accountUuid, door.price(), UUID.randomUUID(), door.doorUuid());
                entityMessageService.sendToEntity(ctx.session(),player.entityId(), "warp", Variants.of(door.warpTarget()));
                player.sendMessage("Вы заплатили %s %s".formatted(withdraw.getAmount(), getDeclinedAmount(withdraw.getAmount()) ));
            } catch (InsufficientFundsException e) {
                player.sendMessage("Недостаточно средств.");
            } catch (Exception e) {
                log.warn(e.getMessage());
                player.sendMessage(e.getMessage());
            }

        });


    }
    private String getDeclinedAmount(long amount) {
        return RussianLiteralsUtils.declineWord((int) amount, "монета", "монеты", "монет");
    }

}
