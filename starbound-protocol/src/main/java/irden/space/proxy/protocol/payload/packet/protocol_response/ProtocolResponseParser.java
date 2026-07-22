package irden.space.proxy.protocol.payload.packet.protocol_response;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class ProtocolResponseParser implements PacketParser<ProtocolResponse> {
    @Override
    public ProtocolResponse parse(BinaryReader reader) {
        int serverResponse = reader.readUnsignedByte();
        VariantValue info = reader.hasRemaining() ? VariantCodec.INSTANCE.read(reader) : null;
        return new ProtocolResponse(serverResponse, info);
    }

    @Override
    public byte[] write(BinaryWriter writer, ProtocolResponse payload) {
        writer.writeByte(payload.serverResponse());
        if (payload.info() != null) {
            VariantCodec.INSTANCE.write(writer, payload.info());
        }
        return finish(writer);
    }
}
