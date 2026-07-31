package irden.space.proxy.plugin.star_custom_chat;

import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.codec.variant.Variants;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
@Slf4j
@RequiredArgsConstructor
public class SCCCommandRequestInterceptor {
    private static final String CUSTOM_CHAT_ENTITY_TYPE = "irdencustomchat";
    private static final String REQUEST_COMMANDS_MESSAGE = "requestCommands";
    private static final String COMMAND_LIST_MESSAGE = "scc_stagehand_commandlist";
    private static final String COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE = "irdenchat:command:autocomplete";
    private static final String COMMAND_RUN_MESSAGE = "irdenchat:command:run";
    private static final String SERVER_NAME = "IrdenServer";
    private final CommandHandlerPlugin commandHandlerPlugin;
    private final EntityMessageService entityMessages;
    private final SccCommandAutocomplete commandAutocomplete;


    @EntityMessageHandler(COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE)
    public VariantValue autocompleteRequest(EntityMessageContext context) {
        String substr = Variants.asString(context.arg(0)).orElse(null);
        if (substr == null || substr.isBlank()) {
            return Variants.list();
        }
        return commandAutocomplete.suggest(context.session(), substr);
    }

    @EntityMessageHandler(COMMAND_RUN_MESSAGE)
    public VariantValue runCommand(EntityMessageContext context) {
        String text = Variants.asString(context.arg(0)).orElse(null);
        if (text == null || text.isBlank()) {
            return handled(false);
        }

        String cleaned = StarCustomChatCommandExporter.stripArgumentLabels(text);
        if (cleaned == null || !cleaned.startsWith(CommandHandlerPlugin.COMMAND_PREFIX)) {
            return handled(false);
        }

        PacketInterceptionContext packetContext = new PacketInterceptionContext(
                context.session(),
                null,
                new ChatSent(cleaned, null, null),
                PacketDirection.TO_SERVER
        );

        PacketDecision decision = commandHandlerPlugin.onChatSent(packetContext);
        boolean wasHandled = decision != null && decision.isDrop();
        if (!wasHandled) {
            log.debug("SCC command '{}' is not registered on the proxy, letting the client fall back", cleaned);
        }
        return handled(wasHandled);
    }

    private static VariantValue handled(boolean handled) {
        return Variants.mapBuilder().put("handled", handled).build();
    }


    @PacketHandler(value = PacketType.SPAWN_ENTITY, direction = PacketDirection.TO_SERVER)
    public PacketDecision onSpawnEntity(PacketInterceptionContext context) {
        var payload = context.parsedPayload();
        if (!(payload instanceof StageHandEntity(VariantValue stagehandPayload))) {
            return PacketDecision.forward();
        }

        JsonNode jsonNode = MapVariantUtils.variantToJsonNode(stagehandPayload);
        if (jsonNode == null) {
            return PacketDecision.forward();
        }

        if (!CUSTOM_CHAT_ENTITY_TYPE.equals(jsonNode.path("type").asString())) {
            return PacketDecision.forward();
        }

        if (!REQUEST_COMMANDS_MESSAGE.equals(jsonNode.path("message").asString())) {
            return PacketDecision.forward();
        }

        JsonNode playerIdNode = jsonNode.path("data").path("playerId");
        if (!playerIdNode.canConvertToInt()) {
            log.warn("Failed to export StarCustomChat commands: request payload does not contain a valid data.playerId");
            return PacketDecision.forward();
        }

        int playerId = playerIdNode.asInt();
        VariantValue[] commandListPayload = {
                StarCustomChatCommandExporter.export(
                        commandHandlerPlugin.allCommands(),
                        context.session().permissions()
                ),
                new StringVariantValue(SERVER_NAME)
        };
        log.info("Received command list request from playerId={}, sending command list response with {} commands", playerId, commandListPayload.length);

        return PacketDecision.cancel(() -> entityMessages.sendToEntity(context.session(), playerId, COMMAND_LIST_MESSAGE, commandListPayload));
    }
}
