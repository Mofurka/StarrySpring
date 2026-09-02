package irden.space.proxy.protocol.payload.packet.tile_liquid_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.liquid.LiquidNetUpdate;
import irden.space.proxy.protocol.payload.common.liquid.LiquidNetUpdateCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class TileLiquidUpdateParser implements PacketParser<TileLiquidUpdate> {

    @Override
    public TileLiquidUpdate parse(BinaryReader reader) {
        int x = VlqCodec.INSTANCE.readInt(reader);
        int y = VlqCodec.INSTANCE.readInt(reader);
        LiquidNetUpdate liquidUpdate = LiquidNetUpdateCodec.INSTANCE.read(reader);
        return new TileLiquidUpdate(new StarVec2I(x, y), liquidUpdate);
    }

    @Override
    public byte[] write(BinaryWriter writer, TileLiquidUpdate payload) {
        VlqCodec.INSTANCE.write(writer, payload.position().x());
        VlqCodec.INSTANCE.write(writer, payload.position().y());
        LiquidNetUpdateCodec.INSTANCE.write(writer, payload.liquidUpdate());
        return finish(writer);
    }
}
