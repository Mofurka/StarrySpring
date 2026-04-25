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
    public ChatReceive parse(BinaryReader reader) {
        ChatHeader header = ChatHeaderCodec.read(reader);
        String name = StarStringCodec.INSTANCE.read(reader);
        reader.readUnsignedByte();
        String message = StarStringCodec.INSTANCE.read(reader);
        VariantValue[] variantValues = null;
        if (reader.openProtocolVersion() >= 5) {
            variantValues = VariantCodec.INSTANCE.readList(reader);
        }
        return new ChatReceive(
                header,
                name,
                message,
                variantValues

        );
    }

    @Override
    public byte[] write(BinaryWriter writer, ChatReceive payload) {
        ChatHeaderCodec.write(writer, payload.header());
        StarStringCodec.INSTANCE.write(writer, payload.name());
        writer.writeByte(0);
        StarStringCodec.INSTANCE.write(writer, payload.message());
        if (writer.openProtocolVersion() >= 5) {
            VariantValue[] data = payload.data();
            if (data == null) {
                data = new VariantValue[0];
            }
            VariantCodec.INSTANCE.writeList(writer, data);
        }
        return finish(writer);
    }
}
