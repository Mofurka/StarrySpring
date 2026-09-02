package irden.space.proxy.protocol.payload.packet.tile_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.payload.common.net_tile.NetTile;
import irden.space.proxy.protocol.payload.common.net_tile.NetTileCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.registry.PacketParser;

public class TileUpdateParser implements PacketParser<TileUpdate> {

    @Override
    public TileUpdate parse(BinaryReader reader) {
        int x = VlqCodec.INSTANCE.readInt(reader);
        int y = VlqCodec.INSTANCE.readInt(reader);
        NetTile tile = NetTileCodec.INSTANCE.read(reader);
        return new TileUpdate(new StarVec2I(x, y), tile);
    }

    @Override
    public byte[] write(BinaryWriter writer, TileUpdate payload) {
        VlqCodec.INSTANCE.write(writer, payload.position().x());
        VlqCodec.INSTANCE.write(writer, payload.position().y());
        NetTileCodec.INSTANCE.write(writer, payload.tile());
        return finish(writer);
    }
}
