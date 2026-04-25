package irden.space.proxy.plugin.command_handler;

import com.fasterxml.jackson.databind.JsonNode;
import irden.space.proxy.plugin.api.*;
import irden.space.proxy.plugin.api.annotations.OnLoad;
import irden.space.proxy.plugin.api.annotations.PacketHandler;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.ListVariantValue;
import irden.space.proxy.protocol.codec.variant.MapVariantValue;
import irden.space.proxy.protocol.codec.variant.StringVariantValue;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.star_uuid.StarUuid;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.entity.type.Entity;
import irden.space.proxy.protocol.payload.packet.entity.type.StageHandEntity;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityIdTarget;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessage;
import irden.space.proxy.protocol.payload.packet.entity_message.EntityMessageTarget;
import irden.space.proxy.protocol.util.MapVariantUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@PluginDefinition(
        id = "command-handler",
        name = "Command Handler",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "A plugin that allows you to handle commands sent by clients."
)
public class CommandHandlerPlugin implements ProxyPlugin {

    private static final String COMMAND_PREFIX = "/";
    private static final Logger log = LoggerFactory.getLogger(CommandHandlerPlugin.class);

    private final CommandParser commandParser = new CommandParser();

    @OnLoad
    public void handleLoad(PluginContext context) {
        context.publishService(CommandHandlerPlugin.class, this);
    }

    @PacketHandler(value = PacketType.SPAWN_ENTITY, direction = PacketDirection.TO_SERVER)
    public PacketDecision onSpawnEntity(PacketInterceptionContext context) {
        var payload = context.parsedPayload();
        if (payload instanceof StageHandEntity(VariantValue payload1)) {
            JsonNode jsonNode = MapVariantUtils.variantToJsonNode(payload1);
            String type = jsonNode.get("type").asText();
            if (type.equals("irdencustomchat")) {
                String message = jsonNode.get("message").asText();
                if (message.equals("requestCommands")) {

                    /*{
                      "data" : {
                        "playerId" : -2555904
                      },
                      "name" : "",
                      "debug" : true,
                      "scripts" : [ "/scripts/quest/messaging.lua", "/stagehands/mailbox.lua", "/stagehands/irden/irdencustomchathandler.lua" ],
                      "message" : "requestCommands",
                      "type" : "irdencustomchat",
                      "scriptDelta" : 1
                    }*/

                    /* answer
                    *     world.sendEntityMessage(data.playerId, "scc_stagehand_commandlist", {
                          { command = "/myNewCommand", description = "New server command" },
                          { command = "/compoundCommand", description = "Main command", subcommands = {
                            option1, option2
                          } }
                        })
                    * */
                    int playerId = jsonNode.get("data").get("playerId").asInt();
                    var testCommands =
                            new VariantValue[]{
                                    new ListVariantValue(
                                            new VariantValue[] {
                                                    new MapVariantValue(
                                                            Map.of(
                                                                    "command", new StringVariantValue("/myNewCommand"),
                                                                    "description", new StringVariantValue("New server command")
                                                            )
                                                    ),
                                                    new MapVariantValue(
                                                            Map.of(
                                                                    "command", new StringVariantValue("/compoundCommand"),
                                                                    "description", new StringVariantValue("Main command"),
                                                                    "subcommands", new ListVariantValue(new VariantValue[]{

                                                                            new MapVariantValue(
                                                                                    Map.of(
                                                                                            "command", new StringVariantValue("option1"),
                                                                                            "description", new StringVariantValue("Option 1")
                                                                                    )
                                                                            ),
                                                                            new MapVariantValue(
                                                                                    Map.of(
                                                                                            "command", new StringVariantValue("option2"),
                                                                                            "description", new StringVariantValue("Option 2")
                                                                                    )
                                                                            )
                                                                    }
                                                                    )
                                                            )
                                                    )
                                            }
                                    ),
                                    new StringVariantValue("IrdenServer")
                            };

                    EntityMessage sccStagehandCommandlist = EntityMessage.builder()
                            .entityId(new EntityIdTarget(playerId))
                            .message("scc_stagehand_commandlist")
                            .args(testCommands)
                            .uuid(StarUuid.fromJavaUuid(UUID.randomUUID()))
                            .fromConnection(0)
                            .build();

                    context.session().sendToClient(
                            PacketType.ENTITY_MESSAGE,
                            sccStagehandCommandlist
                    );
                    return PacketDecision.cancel();


                }
            }
        }
        return PacketDecision.forward();
    }


    @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onChatSent(PacketInterceptionContext context) {
        ChatSent chatSent = (ChatSent) context.parsedPayload();
        String content = chatSent.content();

        if (content.isBlank() || !content.startsWith(COMMAND_PREFIX)) {
            return PacketDecision.forward();
        }

        String commandLine = content.substring(COMMAND_PREFIX.length()).trim();

        if (commandLine.isBlank()) {
            return PacketDecision.forward();
        }

        ParsedCommand parsedCommand = parse(commandLine);
        RegisteredCommand registeredCommand = CommandRegistry.global().find(parsedCommand.commandName());

        if (registeredCommand == null) {
            return PacketDecision.forward();
        }

        log.info(
                "Executing command '/{}' from plugin '{}'",
                registeredCommand.name(),
                registeredCommand.ownerPluginId()
        );

        CommandParseResult parseResult = commandParser.parse(
                registeredCommand.root(),
                parsedCommand.argumentsLine(),
                parsedCommand.tokens()
        );
        if (parseResult instanceof CommandParseResult.Error(String message)) {
            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage(message)
            );
            return PacketDecision.cancel();
        }

        CommandParseResult.Success success = (CommandParseResult.Success) parseResult;

        List<String> rawArguments = parsedCommand.tokens()
                .stream()
                .map(CommandToken::value)
                .toList();

        CommandContext commandContext = new CommandContext(
                context,
                registeredCommand.name(),
                commandLine,
                parsedCommand.argumentsLine(),
                rawArguments,
                success.arguments()
        );

        try {
            success.executor().execute(commandContext);
            return PacketDecision.cancel();
        } catch (RuntimeException e) {
            log.error("Failed to execute command '/{}'", registeredCommand.name(), e);

            context.session().sendToClient(
                    PacketType.CHAT_RECEIVE,
                    CommandMessages.systemMessage("Command '/" + registeredCommand.name() + "' failed: " + e.getMessage())
            );

            return PacketDecision.cancel();
        }
    }

    private ParsedCommand parse(String commandLine) {
        String[] parts = commandLine.split("\\s+", 2);

        String commandName = parts[0].trim().toLowerCase(Locale.ROOT);
        String argumentsLine = parts.length > 1 ? parts[1] : "";

        List<CommandToken> tokens = CommandTokenizer.tokenize(argumentsLine);

        return new ParsedCommand(commandName, argumentsLine, tokens);
    }

    private record ParsedCommand(
            String commandName,
            String argumentsLine,
            List<CommandToken> tokens
    ) {
    }
}