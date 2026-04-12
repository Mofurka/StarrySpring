package irden.space.proxy.protocol.payload.packet.chat;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeader;
import irden.space.proxy.protocol.payload.common.chat_header.ChatHeaderCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.List;

public class ChatRecieveParser implements PacketParser<ChatReceive> {

    @Override
    public ChatReceive parse(BinaryReader reader, int openProtocolVersion) {
        ChatHeader header = ChatHeaderCodec.INSTANCE.read(reader);
        String name = StarStringCodec.read(reader);
        int junk = reader.readUnsignedByte();
        String message = StarStringCodec.read(reader);
        List<VariantValue> variantValues = null;
        if (openProtocolVersion >= 5) {
            variantValues = VariantCodec.readList(reader);
        }
        return new ChatReceive(
                header,
                name,
                junk,
                message,
                variantValues

        );
    }

    @Override
    public byte[] write(ChatReceive payload, int openProtocolVersion) {
        BinaryWriter writer = new BinaryWriter();
        ChatHeaderCodec.INSTANCE.write(writer, payload.header());
        StarStringCodec.write(writer, payload.name());
        writer.writeByte(payload.junk());
        StarStringCodec.write(writer, payload.message());
        if (openProtocolVersion >= 5) {
            List<VariantValue> data = payload.data();
            if (data == null) {
                data = List.of();
            }
            VariantCodec.writeList(writer, data);
        }
        return finish(writer);
    }
}
