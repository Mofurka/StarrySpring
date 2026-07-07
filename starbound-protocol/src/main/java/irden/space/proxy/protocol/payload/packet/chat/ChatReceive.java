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
        if (!(obj instanceof ChatReceive(ChatHeader header1, String name1, String message1, VariantValue[] data1))) {
            return false;
        }
        return Objects.equals(header, header1)
                && Objects.equals(name, name1)
                && Objects.equals(message, message1)
                && Arrays.equals(data, data1);
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
