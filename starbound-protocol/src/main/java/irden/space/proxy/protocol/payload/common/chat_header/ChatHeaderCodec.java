package irden.space.proxy.protocol.payload.common.chat_header;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.StarStringCodec;
import irden.space.proxy.protocol.payload.packet.chat.consts.ChatReceiveMode;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ChatHeaderCodec {


    public static ChatHeader read(BinaryReader reader) {
        ChatReceiveMode mode = ChatReceiveMode.fromId(reader.readUnsignedByte());

        if (mode.equals(ChatReceiveMode.LOCAL) || mode.equals(ChatReceiveMode.PARTY)) {
            String channel = StarStringCodec.read(reader);
            int clientId = reader.readUInt16BE();
            return new ChatHeader(mode, channel, clientId);
        }

        reader.readUnsignedByte(); // reserved / junk byte
        int clientId = reader.readUInt16BE();
        return new ChatHeader(mode, null, clientId);
    }


    public static void write(BinaryWriter writer, ChatHeader header) {
        writer.writeByte(header.mode().id());

        if (header.mode().equals(ChatReceiveMode.LOCAL) || header.mode().equals(ChatReceiveMode.PARTY)) {
            StarStringCodec.write(writer, header.channel());
            writer.writeUInt16BE(header.clientId());
            return;
        }

        writer.writeByte(0);
        writer.writeUInt16BE(header.clientId());
    }
}
