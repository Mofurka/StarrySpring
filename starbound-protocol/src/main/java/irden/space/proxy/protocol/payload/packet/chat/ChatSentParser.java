package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.List;

public class ChatSentParser implements PacketParser<ChatSent> {
    @Override
    public ChatSent parse(BinaryReader reader) {
        String read = StarStringCodec.INSTANCE.read(reader);
        ChatSentMode chatSentMode = ChatSentMode.fromId(reader.readUnsignedByte());
        List<VariantValue> variantValues = null;
        if (reader.openProtocolVersion() >= 5) {
            variantValues = VariantCodec.INSTANCE.readList(reader);
        }
        return new ChatSent(
                read,
                chatSentMode,
                variantValues
        );
    }

    @Override
    public byte[] write(BinaryWriter writer, ChatSent payload) {
        StarStringCodec.INSTANCE.write(writer, payload.content());
        writer.writeByte(payload.mode().id());
        if (writer.openProtocolVersion() >= 5) {
            List<VariantValue> arguments = payload.arguments();
            if (arguments == null) {
                arguments = List.of();
            }
            VariantCodec.INSTANCE.writeList(writer, arguments);
        }
        return finish(writer);
    }
}
