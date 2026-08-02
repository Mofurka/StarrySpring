package irden.space.proxy.protocol.payload.packet.request_drop;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class RequestDropParser implements PacketParser<RequestDrop> {

    @Override
    public RequestDrop parse(BinaryReader reader) {
        return new RequestDrop(VlqCodec.INSTANCE.readInt(reader));
    }

    @Override
    public byte[] write(BinaryWriter writer, RequestDrop payload) {
        VlqCodec.INSTANCE.write(writer, payload.dropEntityId());
        return finish(writer);
    }
}
