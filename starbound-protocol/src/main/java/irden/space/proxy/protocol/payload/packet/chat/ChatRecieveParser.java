package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeaderCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ChatRecieveParser implements PacketParser<ChatReceive> {

    @Override
    public ChatReceive parse(BinaryReader reader) {
        ChatHeader header = ChatHeaderCodec.INSTANCE.read(reader);
        String name = StarStringCodec.read(reader);
        int junk = reader.readUnsignedByte();
        String message = StarStringCodec.read(reader);

        return new ChatReceive(
                header,
                name,
                junk,
                message,
                VariantCodec.readList(reader)
        );
    }

    @Override
    public byte[] write(ChatReceive payload) {
        BinaryWriter writer = new BinaryWriter();

        ChatHeaderCodec.INSTANCE.write(writer, payload.header());
        StarStringCodec.write(writer, payload.name());
        writer.writeByte(payload.junk());
        StarStringCodec.write(writer, payload.message());
        VariantCodec.writeList(writer, payload.data());
        return finish(writer);
    }
}
