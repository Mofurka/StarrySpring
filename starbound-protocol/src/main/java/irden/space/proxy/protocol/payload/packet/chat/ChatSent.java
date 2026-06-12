package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;

import java.util.Arrays;
import java.util.Objects;

public record ChatSent(
        String content,
        ChatSentMode mode,
        VariantValue[] arguments
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChatSent other)) {
            return false;
        }
        return Objects.equals(content, other.content)
                && mode == other.mode
                && Arrays.equals(arguments, other.arguments);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(content, mode);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }

    @Override
    public String toString() {
        return "ChatSent[content=" + content
                + ", mode=" + mode
                + ", arguments=" + Arrays.toString(arguments)
                + ']';
    }
}
