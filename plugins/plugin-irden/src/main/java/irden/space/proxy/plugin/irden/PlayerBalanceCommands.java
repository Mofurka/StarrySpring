package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.plugin.command_handler.wording.RussianLiteralsUtils;
import irden.space.proxy.plugin.irden.constants.PlayerAccountDefaults;
import irden.space.proxy.plugin.irden.persistence.model.AccountEntity;
import irden.space.proxy.plugin.irden.persistence.model.AccountTransactionEntity;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.irden.service.AccountTransactionService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTarget;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceCommands {
    private final PlayerManagerApi playerManagerApi;
    private final PlayerBalanceCommandsHandler playerBalanceCommandsHandler;

    // Пока что для обратной совместимости, и потому что игроки привыклю, добавлю дефолтные команды на вроде /money
    @ChatCommand("money")
    public CommandSpec money() {
        return CommandSpec.literal("money").executes(playerBalanceCommandsHandler::handleMoneyCommand)
                .then(
                        CommandSpec.literal("give")
                                .then(
                                        CommandSpec.argument("player", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi))
                                                .then(
                                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                                .then(
                                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(playerBalanceCommandsHandler::handleMoneyGiveCommand)
                                                                )
                                                )
                                )
                )
                .then(
                        CommandSpec.literal("drop")
                                .then(
                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                .then(
                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(playerBalanceCommandsHandler::handleMoneyDropCommand)
                                                )
                                )
                )
                .build();
    }


}
