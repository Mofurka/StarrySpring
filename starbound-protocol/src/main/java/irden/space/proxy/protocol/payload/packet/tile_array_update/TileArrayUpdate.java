package irden.space.proxy.protocol.payload.packet.tile_array_update;

import irden.space.proxy.protocol.payload.common.net_tile.NetTile;
import irden.space.proxy.protocol.payload.common.vectors.StarVec2I;

import java.util.List;

public record TileArrayUpdate(
        StarVec2I min,
        int width,
        int height,
        List<NetTile> tiles
) {

    public NetTile tileAt(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Tile (" + x + ", " + y + ") is outside of " + width + "x" + height);
        }
        return tiles.get(y * width + x);
    }

    /**
     * Абсолютные координаты тайла в мире.
     */
    public StarVec2I positionAt(int x, int y) {
        return new StarVec2I(min.x() + x, min.y() + y);
    }
}
