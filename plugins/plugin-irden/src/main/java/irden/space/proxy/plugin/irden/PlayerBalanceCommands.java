package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.irden.permissions.BalancePermissions;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerOnlineTargetArgumentType;
import irden.space.proxy.plugin.player_manager.command.PlayerTargetArgumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerBalanceCommands {
    private final PlayerManagerApi playerManagerApi;
    private final PlayerBalanceCommandsHandler playerBalanceCommandsHandler;

    // Пока что для обратной совместимости, и потому что игроки привыкли, добавлю дефолтные команды на вроде /money
    @ChatCommand("money")
    public CommandSpec money() {
        return CommandSpec.literal("money")
                .surfaces(CommandSurface.IN_GAME)
                .executes(playerBalanceCommandsHandler::handleMoneyCommand)
                .then(
                        CommandSpec.literal("give")
                                .then(payArgument())
                )
                .then(
                        CommandSpec.literal("drop")
                                .then(
                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                .then(
                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.")
                                                                .optional()
                                                                .executes(playerBalanceCommandsHandler::handleMoneyDropCommand)
                                                )
                                )
                )
                .then(
                        CommandSpec.literal("history").description("Показать историю операций по вашему счёту.")
                                .executes(playerBalanceCommandsHandler::handleMoneyHistoryCommand)
                                .then(
                                        CommandSpec.argument("page", IntegerArgumentType.integer()).description("Номер страницы.")
                                                .optional()
                                                .executes(playerBalanceCommandsHandler::handleMoneyHistoryCommand)
                                )
                )
                .then(
                        CommandSpec.literal("top").description("Самые богатые игроки.").permission(BalancePermissions.BALANCE_MANAGEMENT)
                                .executes(playerBalanceCommandsHandler::handleMoneyTopCommand)
                                .then(
                                        CommandSpec.argument("count", IntegerArgumentType.integer()).description("Сколько мест показать.")
                                                .optional()
                                                .executes(playerBalanceCommandsHandler::handleMoneyTopCommand)
                                )
                )
                .build();
    }

    @ChatCommand(value = "balance", aliases = "bal", description = "Показать ваш баланс.")
    public CommandSpec balance() {
        return CommandSpec.literal("balance")
                .surfaces(CommandSurface.IN_GAME)
                .executes(playerBalanceCommandsHandler::handleMoneyCommand)
                .build();
    }

    @ChatCommand(value = "pay", description = "Передать деньги другому игроку.")
    public CommandSpec pay() {
        return CommandSpec.literal("pay")
                .surfaces(CommandSurface.IN_GAME)
                .then(payArgument())
                .build();
    }


    @ChatCommand(value = "eco", aliases = "economy", description = "Администрирование счетов: выдача и списание денег.")
    public CommandSpec eco() {
        return CommandSpec.literal("eco").permission(BalancePermissions.BALANCE_MANAGEMENT)
                .then(
                        CommandSpec.literal("give").description("Начислить деньги игроку.")
                                .then(adminAmountArgument(playerBalanceCommandsHandler::handleEcoGiveCommand))
                )
                .then(
                        CommandSpec.literal("take").description("Списать деньги у игрока.")
                                .then(adminAmountArgument(playerBalanceCommandsHandler::handleEcoTakeCommand))
                )
                .then(
                        CommandSpec.literal("transfer").description("Перевести деньги между игроками.")
                                .then(
                                        CommandSpec.argument("from", PlayerTargetArgumentType.playerTarget(playerManagerApi))
                                                .then(
                                                        CommandSpec.argument("to", PlayerTargetArgumentType.playerTarget(playerManagerApi))
                                                                .then(
                                                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                                                .then(
                                                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.")
                                                                                                .optional()
                                                                                                .executes(playerBalanceCommandsHandler::handleEcoTransferCommand)
                                                                                )
                                                                )
                                                )
                                )
                )
                .build();
    }


    private CommandNodeBuilder<?> payArgument() {
        return CommandSpec.argument("player", PlayerOnlineTargetArgumentType.playerTarget(playerManagerApi))
                .then(
                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                .then(
                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(playerBalanceCommandsHandler::handleMoneyGiveCommand)
                                )
                );
    }


    private CommandNodeBuilder<?> adminAmountArgument(CommandExecutor executor) {
        return CommandSpec.argument("player", PlayerTargetArgumentType.playerTarget(playerManagerApi))
                .then(
                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                .then(
                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(executor)
                                )
                );
    }
}
