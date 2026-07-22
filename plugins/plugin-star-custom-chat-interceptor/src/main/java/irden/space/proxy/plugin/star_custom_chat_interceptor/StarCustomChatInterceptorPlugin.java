package irden.space.proxy.plugin.star_custom_chat_interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import irden.space.proxy.plugin.api.PacketDecision;
import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginDefinition;
import irden.space.proxy.plugin.api.ProxyPlugin;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageContext;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageHandler;
import irden.space.proxy.plugin.command_handler.entity_message.EntityMessageService;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.util.MapVariantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@PluginDefinition(
        id = "star-custom-chat-interceptor",
        name = "Star Custom Chat Interceptor",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager", "general"},
        author = "https://github.com/Mofurka",
        description = "A star custom chat interceptor plugin that can handle star custom chat messages via stagehand entity spawnd and ems."
)
@Component
@RequiredArgsConstructor
@Slf4j
public final class StarCustomChatInterceptorPlugin implements ProxyPlugin {
    private static final String CUSTOM_CHAT_ENTITY_TYPE = "irdencustomchat";
    private static final String REQUEST_COMMANDS_MESSAGE = "requestCommands";
    private static final String COMMAND_LIST_MESSAGE = "scc_stagehand_commandlist";
    private static final String COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE = "irdenchat_command_autocomplete";
    private static final String SERVER_NAME = "IrdenServer";
    private final CommandHandlerPlugin commandHandlerPlugin;
    private final EntityMessageService entityMessages;


    @OnLoad
    public void handleLoad() {
        log.info("Loading plugin '{}'", descriptor().id());
    }


    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }

    @EntityMessageHandler(COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE)
    public void autocompleteRequest(EntityMessageContext context) {
        log.info("Received command autocomplete request: {}", context.message());
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

        if (!CUSTOM_CHAT_ENTITY_TYPE.equals(jsonNode.path("type").asText())) {
            return PacketDecision.forward();
        }

        if (!REQUEST_COMMANDS_MESSAGE.equals(jsonNode.path("message").asText())) {
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
