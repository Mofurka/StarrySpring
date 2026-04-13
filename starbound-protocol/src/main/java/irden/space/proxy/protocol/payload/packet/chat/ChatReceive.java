package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;

import java.util.List;

public record ChatReceive(
    ChatHeader header,
    String name,
    String message,
    List<VariantValue> data
) {
}
