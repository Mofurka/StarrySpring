package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import lombok.Builder;

import java.util.Arrays;
import java.util.Objects;

@Builder
public record ChatReceive(
    ChatHeader header,
    String name,
    String message,
    VariantValue[] data
) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChatReceive other)) {
            return false;
        }
        return Objects.equals(header, other.header)
                && Objects.equals(name, other.name)
                && Objects.equals(message, other.message)
                && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(header, name, message);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "ChatReceive[header=" + header
                + ", name=" + name
                + ", message=" + message
                + ", data=" + Arrays.toString(data)
                + ']';
    }
}
