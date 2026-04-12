package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import lombok.NonNull;

import java.util.List;

public record ChatSent(
        @NonNull String content,
        ChatSentMode mode,
        List<VariantValue> arguments
) {
}
