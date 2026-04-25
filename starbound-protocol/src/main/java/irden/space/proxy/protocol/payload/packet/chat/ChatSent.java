package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;

import java.util.List;

public record ChatSent(
        String content,
        ChatSentMode mode,
        VariantValue[] arguments
) {
}
