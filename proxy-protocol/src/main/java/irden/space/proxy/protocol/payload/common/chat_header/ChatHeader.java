package irden.space.proxy.protocol.payload.common.chat_header;

import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;

public record ChatHeader(
        ChatReceiveMode mode,
        String channel,
        int clientId
) {
}
