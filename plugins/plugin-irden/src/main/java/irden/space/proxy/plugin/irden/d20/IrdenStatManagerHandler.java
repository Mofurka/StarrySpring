package irden.space.proxy.plugin.irden.d20;

import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.irden.d20.constants.MessageTemplate;
import irden.space.proxy.plugin.irden.d20.constants.MessageType;
import irden.space.proxy.plugin.irden.d20.constants.RollMode;
import irden.space.proxy.plugin.player_manager.api.PlayerManagerApi;
import irden.space.proxy.plugin.player_manager.model.Player;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class IrdenStatManagerHandler {

    private static final String STAT_MANAGER_VERSION = "4.25.15";
    private static final int EXPECTED_MAJOR_VERSION =
            Integer.parseInt(STAT_MANAGER_VERSION.split("\\.")[0]);

    private final PlayerManagerApi playerManagerApi;
    private final RollMessageBuilder rollMessageBuilder;
    private final MoneyMessageHandler moneyMessageHandler;
    private final ISMMessenger messenger;

    private static Integer majorVersion(String version) {
        String head = version.split("\\.", 2)[0];
        try {
            return Integer.parseInt(head.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @EntityMessageHandler("statmanager")
    public void handleStatManager(EntityMessageContext message) {
        VariantValue arg = message.arg(0);
        StatManagerMessage info = StatManagerMessage.from(arg);
        if (info == null) {
            // Не наш формат (нет поля entityType) - молча игнорируем, как и Python.
            return;
        }

        Player sender = playerManagerApi.findPlayerBySessionId(message.session().sessionId()).orElse(null);
        if (sender == null) {
            log.debug("statmanager message from unknown session {}", message.session().sessionId());
            return;
        }

        try {
            info.player(sender);
            processEntityMessage(sender, info);
        } catch (Exception err) {
            log.debug("Failed to process statmanager message {}: {}", arg, err, err);
        }
    }

    private void processEntityMessage(Player sender, StatManagerMessage info) {
        if (!checkVersion(sender, info)) {
            return;
        }

        String type = info.type();
        String result = switch (type) {
            case MessageType.ACTION_ROLL -> rollMessageBuilder.actionRoll(info);
            case MessageType.STAT_ROLL -> rollMessageBuilder.statRoll(info);
            case MessageType.DICE_ROLL -> rollMessageBuilder.diceRoll(info);
            case MessageType.INITIATIVE -> rollMessageBuilder.initiative(info);
            case MessageType.RESOURCE_EVENT -> rollMessageBuilder.resourceEvent(info);
            case MessageType.RETURN_TO_FIGHT -> {
                handleReturnToFight(sender, info);
                yield null;
            }
            case MessageType.SHOW_MONEY -> {
                moneyMessageHandler.showMoney(sender, info);
                yield null;
            }
            case MessageType.TRANSFER_MONEY -> {
                moneyMessageHandler.transferMoney(sender, info);
                yield null;
            }
            default -> {
                log.warn("Unknown statmanager message entityType: {}", type);
                yield null;
            }
        };

        if (result != null && !result.isEmpty()) {
            dispatchMessage(sender, result, info);
        }
    }

    private void handleReturnToFight(Player sender, StatManagerMessage info) {
        String message = MessageTemplate.RETURN_TO_FIGHT
                .replace("{fight_name}", safe(info.fightName()))
                .replace("{initiative}", Integer.toString(info.initiative()));
        messenger.sendMessage(sender, List.of(sender.clientId()), message, RollMode.LOCAL, info);
    }

    private boolean checkVersion(Player sender, StatManagerMessage info) {
        String version = info.version();
        Integer major = version == null ? null : majorVersion(version);
        if (major == null || major != EXPECTED_MAJOR_VERSION) {
            messenger.sendMessage(
                    sender,
                    List.of(sender.clientId()),
                    MessageTemplate.UPDATE_VERSION.replace("{version}", STAT_MANAGER_VERSION),
                    RollMode.LOCAL, info
            );
            return false;
        }
        return true;
    }

    private void dispatchMessage(Player sender, String message, StatManagerMessage info) {
        List<Integer> clientIds = info.clientIds();

        if (info.silent()) {
            messenger.sendMessage(sender, clientIds, message, RollMode.PARTY, info);
            return;
        }

        String rollMode = info.rollMode();
        if (rollMode == null) {
            messenger.sendMessage(sender, clientIds, message, RollMode.BROADCAST, info);
            return;
        }

        switch (rollMode) {
            case RollMode.PARTY -> messenger.sendMessage(sender, clientIds, message, RollMode.PARTY, info);
            case RollMode.SILENT -> {
                if (info.hasTarget()) {
                    messenger.sendMessage(sender, clientIds, message, RollMode.PARTY, info);
                } else {
                    messenger.sendMessage(sender, clientIds, MessageTemplate.SILENT_PREFIX + message, RollMode.SILENT, info);
                }
            }
            case RollMode.LOCAL, RollMode.FIGHT -> messenger.sendMessage(sender, clientIds, message, rollMode, info);
            default -> messenger.sendMessage(sender, clientIds, message, RollMode.BROADCAST, info);
        }
    }
}
