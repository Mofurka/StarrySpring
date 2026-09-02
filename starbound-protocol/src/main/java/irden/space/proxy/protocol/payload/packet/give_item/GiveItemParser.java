package irden.space.proxy.protocol.payload.packet.give_item;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptor;
import irden.space.proxy.protocol.payload.common.star_item.StarItemDescriptorCodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class GiveItemParser implements PacketParser<StarItemDescriptor> {
    @Override
    public StarItemDescriptor parse(BinaryReader reader) {
        return StarItemDescriptorCodec.INSTANCE.read(reader);
    }

    @Override
    public byte[] write(BinaryWriter writer, StarItemDescriptor payload) {
        StarItemDescriptorCodec.INSTANCE.write(writer, payload);
        return finish(writer);
    }
}
