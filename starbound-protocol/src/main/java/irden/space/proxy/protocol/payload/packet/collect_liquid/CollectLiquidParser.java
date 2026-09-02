package irden.space.proxy.protocol.payload.packet.collect_liquid;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2ICodec;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.ArrayList;
import java.util.List;

public class CollectLiquidParser implements PacketParser<CollectLiquid> {

    @Override
    public CollectLiquid parse(BinaryReader reader) {
        int size = VlqUnsignedCodec.INSTANCE.readInt(reader);
        List<StarVec2I> tilePositions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tilePositions.add(StarVec2ICodec.INSTANCE.read(reader));
        }

        int liquidId = reader.readUnsignedByte();

        return new CollectLiquid(tilePositions, liquidId);
    }

    @Override
    public byte[] write(BinaryWriter writer, CollectLiquid payload) {
        VlqUnsignedCodec.INSTANCE.write(writer, payload.tilePositions().size());
        for (StarVec2I tilePosition : payload.tilePositions()) {
            StarVec2ICodec.INSTANCE.write(writer, tilePosition);
        }

        writer.writeByte(payload.liquidId());

        return finish(writer);
    }
}
