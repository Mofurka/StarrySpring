package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatSentMode;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ChatSentParser implements PacketParser<ChatSent> {
    @Override
    public ChatSent parse(BinaryReader reader) {
        return new ChatSent(
                StarStringCodec.read(reader),
                ChatSentMode.fromId(reader.readUnsignedByte()),
                VariantCodec.readList(reader)
        );
    }

    @Override
    public byte[] write(ChatSent payload) {
        BinaryWriter writer = new BinaryWriter();
        StarStringCodec.write(writer, payload.content());
        writer.writeByte(payload.mode().id());
        VariantCodec.writeList(writer, payload.arguments());
        return finish(writer);
    }
}
