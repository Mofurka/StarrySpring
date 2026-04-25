package irden.space.proxy.plugin.command_handler;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.packet.chat.ChatReceive;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
final class CommandMessages {

    static ChatReceive systemMessage(String message) {
        return new ChatReceive(
                new ChatHeader(ChatReceiveMode.COMMAND_RESULT, null, 0),
                "server",
                message,
                new VariantValue[0]
        );
    }
}
