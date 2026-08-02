package irden.space.proxy.protocol.payload.packet.tile_array_update;

import irden.space.proxy.protocol.codec.BinaryReader;
import irden.space.proxy.protocol.codec.BinaryWriter;
import irden.space.proxy.protocol.codec.VlqCodec;
import irden.space.proxy.protocol.codec.VlqUnsignedCodec;
import irden.space.proxy.protocol.payload.common.net_tile.NetTile;
import irden.space.proxy.protocol.payload.common.net_tile.NetTileCodec;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;
import irden.space.proxy.protocol.payload.registry.PacketParser;

import java.util.ArrayList;
import java.util.List;

public class TileArrayUpdateParser implements PacketParser<TileArrayUpdate> {

    @Override
    public TileArrayUpdate parse(BinaryReader reader) {
        int minX = VlqCodec.INSTANCE.readInt(reader);
        int minY = VlqCodec.INSTANCE.readInt(reader);

        int width = VlqUnsignedCodec.INSTANCE.readInt(reader);
        int height = VlqUnsignedCodec.INSTANCE.readInt(reader);

        List<NetTile> tiles = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles.add(NetTileCodec.INSTANCE.read(reader));
            }
        }

        return new TileArrayUpdate(new StarVec2I(minX, minY), width, height, tiles);
    }

    @Override
    public byte[] write(BinaryWriter writer, TileArrayUpdate payload) {
        int expected = payload.width() * payload.height();
        if (payload.tiles().size() != expected) {
            throw new IllegalArgumentException("TileArrayUpdate has " + payload.tiles().size()
                    + " tiles, but " + payload.width() + "x" + payload.height() + " requires " + expected);
        }

        VlqCodec.INSTANCE.write(writer, payload.min().x());
        VlqCodec.INSTANCE.write(writer, payload.min().y());

        VlqUnsignedCodec.INSTANCE.write(writer, payload.width());
        VlqUnsignedCodec.INSTANCE.write(writer, payload.height());

        for (NetTile tile : payload.tiles()) {
            NetTileCodec.INSTANCE.write(writer, tile);
        }

        return finish(writer);
    }
}
