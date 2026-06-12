package irden.space.proxy.plugin.star_custom_chat;

import com.fasterxml.jackson.databind.JsonNode;
import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.OnStop;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.plugin.command_handler.CommandHandlerPlugin;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.util.MapVariantUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@PluginDefinition(
        id = "star-custom-chat",
        name = "Star Custom Chat",
        version = "1.0.0",
        dependsOn = {"command-handler", "player-manager"},
        author = "https://github.com/Mofurka",
        description = "A plugin for star custom chat logging and handle some sort of commands."
)
@Component
public final class StarCustomChatHelperPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(StarCustomChatHelperPlugin.class);
    private static final String CUSTOM_CHAT_ENTITY_TYPE = "irdencustomchat";
    private static final String REQUEST_COMMANDS_MESSAGE = "requestCommands";
    private static final String COMMAND_LIST_MESSAGE = "scc_stagehand_commandlist";
    private static final String COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE = "irdenchat_command_autocomplete";
    private static final String SERVER_NAME = "IrdenServer";

    private CommandHandlerPlugin commandHandlerPlugin;

    @OnLoad
    public void handleLoad(PluginContext context) {
        log.info("Loading plugin '{}'", descriptor().id());
        this.commandHandlerPlugin = context.requireService(CommandHandlerPlugin.class);
    }


    @OnStop
    public void handleStop() {
        log.info("Stopped plugin '{}'", descriptor().id());
    }

    @PacketHandler(value = PacketType.ENTITY_MESSAGE, direction = PacketDirection.TO_SERVER)
    public PacketDecision autocompleteRequest(PacketInterceptionContext context) {
        var em = (EntityMessage) context.parsedPayload();
        if (!COMMAND_AUTOCOMPLETE_REQUEST_MESSAGE.equals(em.message())) {
            return PacketDecision.forward();
        }
        log.info("Received command autocomplete request: {}", em);
        // log for now
        return PacketDecision.cancel();
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

        EntityMessage sccStagehandCommandlist = EntityMessage.builder()
                .entityId(new EntityIdTarget(playerId))
                .message(COMMAND_LIST_MESSAGE)
                .args(commandListPayload)
                .uuid(StarUuid.fromJavaUuid(UUID.randomUUID()))
                .fromConnection(0)
                .build();

        context.session().sendToClient(PacketType.ENTITY_MESSAGE, sccStagehandCommandlist);
        return PacketDecision.cancel();
    }

    @PacketHandler(value = PacketType.PROTOCOL_REQUEST, direction = PacketDirection.TO_SERVER)
    public PacketDecision onProtocolRequest(PacketInterceptionContext context) {
        return PacketDecision.forward();
    }

    @PacketHandler(value = PacketType.PROTOCOL_RESPONSE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onProtocolResponse(PacketInterceptionContext context) {
        return PacketDecision.forward();
    }

}
