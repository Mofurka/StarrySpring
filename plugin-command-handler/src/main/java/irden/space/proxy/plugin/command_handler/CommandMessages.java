package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;

import java.util.List;

final class CommandMessages {

    private CommandMessages() {
    }

    static ChatReceive systemMessage(String message) {
        return new ChatReceive(
                new ChatHeader(ChatReceiveMode.COMMAND_RESULT, null, 0),
                "server",
                0,
                message,
                List.of()
        );
    }
}
