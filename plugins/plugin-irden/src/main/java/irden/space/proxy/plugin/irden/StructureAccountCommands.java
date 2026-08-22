package irden.space.proxy.plugin.irden;

import irden.space.proxy.plugin.command_handler.*;
import irden.space.proxy.plugin.irden.account.StructureAccountArgumentType;
import irden.space.proxy.plugin.irden.account.StructureAccountType;
import irden.space.proxy.plugin.irden.permissions.BalancePermissions;
import irden.space.proxy.plugin.irden.service.AccountService;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.command.PlayerTargetArgumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class StructureAccountCommands {

    private final StructureAccountCommandsHandler handler;
    private final AccountService accountService;
    private final PlayerManagerApi playerManagerApi;

    @ChatCommand(value = "treasury", description = "Счета объектов: крепости, таверны и т.п.")
    public CommandSpec treasury() {
        return CommandSpec.literal("treasury").permission(BalancePermissions.BALANCE_MANAGEMENT)
                .then(
                        CommandSpec.literal("create").description("Создать счёт объекта.")
                                .then(typeAndName(handler::handleCreate))
                )
                .then(
                        CommandSpec.literal("info").description("Показать баланс счёта объекта.")
                                .then(typeAndAccount(handler::handleInfo))
                )
                .then(
                        CommandSpec.literal("deposit").description("Пополнить счёт объекта.")
                                .then(typeAccountAmount(handler::handleDeposit))
                )
                .then(
                        CommandSpec.literal("withdraw").description("Снять со счёта объекта.")
                                .then(typeAccountAmount(handler::handleWithdraw))
                )
                .then(
                        CommandSpec.literal("pay").description("Перевести со счёта объекта игроку.")
                                .then(typeAccountPlayerAmount(handler::handlePay))
                )
                .then(
                        CommandSpec.literal("collect").description("Перевести от игрока на счёт объекта.")
                                .then(typeAccountPlayerAmount(handler::handleCollect))
                )
                .then(
                        CommandSpec.literal("list").description("Список счетов выбранного вида.")
                                .then(
                                        CommandSpec.argument("type", EnumArgumentType.of(StructureAccountType.class))
                                                .executes(handler::handleList)
                                )
                )
                .build();
    }

    private CommandNodeBuilder<?> typeAccountPlayerAmount(CommandExecutor executor) {
        return CommandSpec.argument("type", EnumArgumentType.of(StructureAccountType.class))
                .then(
                        CommandSpec.argument("account", StructureAccountArgumentType.structureAccount(accountService)).description("Имя существующего счёта.")
                                .then(
                                        CommandSpec.argument("player", PlayerTargetArgumentType.playerTarget(playerManagerApi))
                                                .then(
                                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                                .then(
                                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(executor)
                                                                )
                                                )
                                )
                );
    }

    private CommandNodeBuilder<?> typeAndName(CommandExecutor executor) {
        return CommandSpec.argument("type", EnumArgumentType.of(StructureAccountType.class))
                .then(
                        CommandSpec.argument("name", StringArgumentType.word()).description("Имя объекта (в кавычках, если с пробелами).")
                                .executes(executor)
                );
    }

    private CommandNodeBuilder<?> typeAndAccount(CommandExecutor executor) {
        return CommandSpec.argument("type", EnumArgumentType.of(StructureAccountType.class))
                .then(
                        CommandSpec.argument("account", StructureAccountArgumentType.structureAccount(accountService)).description("Имя существующего счёта.")
                                .executes(executor)
                );
    }

    /**
     * {@code <type> <account> <amount> [description]}.
     */
    private CommandNodeBuilder<?> typeAccountAmount(CommandExecutor executor) {
        return CommandSpec.argument("type", EnumArgumentType.of(StructureAccountType.class))
                .then(
                        CommandSpec.argument("account", StructureAccountArgumentType.structureAccount(accountService)).description("Имя существующего счёта.")
                                .then(
                                        CommandSpec.argument("amount", IntegerArgumentType.integer())
                                                .then(
                                                        CommandSpec.argument("description", StringArgumentType.greedyString()).description("Описание транзакции.").optional().executes(executor)
                                                )
                                )
                );
    }
}
