package irden.space.proxy.protocol.payload.protocol_response;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VariantCodec;
import irden.space.proxy.protocol.codec.variant.VariantValue;
import irden.space.proxy.protocol.payload.PacketParser;

public class ProtocolResponseParser implements PacketParser<ProtocolResponse> {
    @Override
    public ProtocolResponse parse(BinaryReader reader) {
        int serverResponse = reader.readUnsignedByte();
        VariantValue info = VariantCodec.read(reader);
        return new ProtocolResponse(serverResponse, info);
    }

    @Override
    public byte[] write(ProtocolResponse payload) {
        BinaryWriter writer = new BinaryWriter();
        writer.writeByte(payload.serverResponse());
        VariantCodec.write(writer, payload.info());
        return finish(writer);
    }
}
