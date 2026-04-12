package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDefinition(
        id = "command-handler",
        name = "Command Handler",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "A plugin that allows you to handle commands sent by clients."
)
public class CommandHandlerPlugin implements ProxyPlugin {
    private final String commandPrefix = "/";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onChatSent(PacketInterceptionContext context) {
        ChatSent chatSent = (ChatSent) context.parsedPayload();
        if (!chatSent.content().isBlank() && chatSent.content().startsWith(commandPrefix)) {
            String command = chatSent.content().substring(commandPrefix.length()).trim();
            log.info("Received command: {}", command);
            return PacketDecision.cancel();
        }
        return PacketDecision.forward();
    }

}
