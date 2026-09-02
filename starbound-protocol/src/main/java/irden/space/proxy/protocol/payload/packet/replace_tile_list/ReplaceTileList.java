package irden.space.proxy.protocol.payload.packet.replace_tile_list;

import irden.space.proxy.protocol.payload.common.tile_damage.TileDamage;
import irden.space.proxy.protocol.payload.packet.modify_tile_list.TileModificationList;

public record ReplaceTileList(
        TileModificationList modificationList,
        TileDamage tileDamage,
        boolean applyDamage
) {
}
