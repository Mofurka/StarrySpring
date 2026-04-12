package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.*;
import irden.space.proxy.protocol.packet.PacketDirection;
import irden.space.proxy.protocol.packet.PacketType;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.ChatSent;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@PluginDefinition(
        id = "command-handler",
        name = "Command Handler",
        version = "1.0.0",
        author = "https://github.com/Mofurka",
        description = "A plugin that allows you to handle commands sent by clients."
)
public class CommandHandlerPlugin implements ProxyPlugin {
    private static final String COMMAND_PREFIX = "/";
    private static final String PLUGIN_COMMAND = "proxy";
    private static final Logger log = LoggerFactory.getLogger(CommandHandlerPlugin.class);


    // Тест отправки команд и перехвата сообщений.
    @PacketHandler(value = PacketType.CHAT_SENT, direction = PacketDirection.TO_SERVER)
    public PacketDecision onChatSent(PacketInterceptionContext context) {
        ChatSent chatSent = (ChatSent) context.parsedPayload();
        String content = chatSent.content();

        if (content.isBlank() || !content.startsWith(COMMAND_PREFIX)) {
            return PacketDecision.forward();
        }

        String commandLine = content.substring(COMMAND_PREFIX.length()).trim();
        log.info("Received command: {}", commandLine);

        if (!commandLine.startsWith(PLUGIN_COMMAND)) {
            return PacketDecision.forward();
        }

        String pluginCommand = commandLine.substring(PLUGIN_COMMAND.length()).trim();

        if (pluginCommand.isBlank() || pluginCommand.equals("help")) {
            context.session().sendToClient(PacketType.CHAT_RECEIVE, helpMessage());
            return PacketDecision.cancel();
        }

        if (pluginCommand.startsWith("send ")) {
            String message = pluginCommand.substring("send ".length()).trim();
            context.session().sendToClient(PacketType.CHAT_RECEIVE, systemMessage(message));
            return PacketDecision.cancel();
        }

        if (pluginCommand.startsWith("rewrite ")) {
            String newMessage = pluginCommand.substring("rewrite ".length()).trim();
            ChatSent modifiedPacket = new ChatSent(newMessage, chatSent.mode(), chatSent.arguments());
            return context.replaceWithPayload(modifiedPacket);
        }

        context.session().sendToClient(
                PacketType.CHAT_RECEIVE,
                systemMessage("Unknown /proxy command. Use /proxy help")
        );
        return PacketDecision.cancel();
    }

    private ChatReceive helpMessage() {
        return systemMessage("Available commands: /proxy send <text>, /proxy rewrite <text>");
    }

    @PacketHandler(value = PacketType.CHAT_RECEIVE, direction = PacketDirection.TO_CLIENT)
    public PacketDecision onChatReceive(PacketInterceptionContext context) {
        ChatReceive chatReceive = (ChatReceive) context.parsedPayload();
        String content = chatReceive.message();
        return PacketDecision.forward();
    }

    private ChatReceive systemMessage(String message) {
        return new ChatReceive(
                new ChatHeader(ChatReceiveMode.COMMAND_RESULT, null, 0),
                "Starry Proxy",
                0,
                message,
                List.of()
        );
    }
}
