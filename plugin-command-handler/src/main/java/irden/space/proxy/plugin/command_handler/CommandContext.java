package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.plugin.api.PacketInterceptionContext;
import irden.space.proxy.plugin.api.PluginSessionContext;
import irden.space.proxy.protocol.packet.PacketType;

import java.util.List;
import java.util.Objects;

public record CommandContext(
        PacketInterceptionContext packetContext,
        String commandName,
        String rawInput,
        String argumentsLine,
        List<String> arguments
) {

    public CommandContext {
        Objects.requireNonNull(packetContext, "packetContext");
        Objects.requireNonNull(commandName, "commandName");
        Objects.requireNonNull(rawInput, "rawInput");
        Objects.requireNonNull(argumentsLine, "argumentsLine");
        arguments = List.copyOf(arguments);
    }

    public PluginSessionContext session() {
        return packetContext.session();
    }

    public void reply(String message) {
        session().sendToClient(PacketType.CHAT_RECEIVE, CommandMessages.systemMessage(message));
    }
}
